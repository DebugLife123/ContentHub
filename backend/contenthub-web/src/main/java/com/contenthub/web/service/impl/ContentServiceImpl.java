package com.contenthub.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.contenthub.common.domain.dos.ContentCategoryDO;
import com.contenthub.common.domain.dos.ContentDO;
import com.contenthub.common.domain.mapper.ContentCategoryMapper;
import com.contenthub.common.domain.mapper.ContentMapper;
import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.jwt.model.LoginUser;
import com.contenthub.web.model.req.ContentPageReqVO;
import com.contenthub.web.model.req.ContentReqVO;
import com.contenthub.web.model.vo.ContentDetailVO;
import com.contenthub.web.model.vo.ContentListVO;
import com.contenthub.web.service.CommentService;
import com.contenthub.web.service.ContentAccessService;
import com.contenthub.web.service.ContentCacheService;
import com.contenthub.web.service.ContentService;
import com.contenthub.web.service.ContentStatService;
import com.contenthub.web.service.FavoriteService;
import com.contenthub.web.service.NotificationService;
import com.contenthub.web.service.ReadingHistoryService;
import com.contenthub.web.util.CurrentUserUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ContentServiceImpl implements ContentService {

    /** 状态流转表：key 为当前状态，value 为允许流转到的状态 */
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "DRAFT", Set.of("PENDING"),
            "REJECTED", Set.of("PENDING"),
            "OFFLINE", Set.of("PENDING"),
            "PENDING", Set.of("PUBLISHED", "REJECTED"),
            "PUBLISHED", Set.of("OFFLINE")
    );

    private final ContentMapper contentMapper;
    private final ContentCategoryMapper categoryMapper;
    private final ContentAccessService accessService;
    private final FavoriteService favoriteService;
    private final CommentService commentService;
    private final ContentCacheService cacheService;
    private final ContentStatService statService;
    private final ReadingHistoryService readingHistoryService;
    private final NotificationService notificationService;

    public ContentServiceImpl(ContentMapper contentMapper,
                              ContentCategoryMapper categoryMapper,
                              ContentAccessService accessService,
                              FavoriteService favoriteService,
                              CommentService commentService,
                              ContentCacheService cacheService,
                              ContentStatService statService,
                              ReadingHistoryService readingHistoryService,
                              NotificationService notificationService) {
        this.contentMapper = contentMapper;
        this.categoryMapper = categoryMapper;
        this.accessService = accessService;
        this.favoriteService = favoriteService;
        this.commentService = commentService;
        this.cacheService = cacheService;
        this.statService = statService;
        this.readingHistoryService = readingHistoryService;
        this.notificationService = notificationService;
    }

    // ------------------------------------------------------------------ 查询

    @Override
    public Response<PageResponse<ContentListVO>> pagePublished(ContentPageReqVO req) {
        return Response.success(doPage(req, null, "PUBLISHED"));
    }

    @Override
    public Response<List<ContentListVO>> hotContents(int limit) {
        List<Long> ids = statService.hotContentIds(limit);
        if (ids.isEmpty()) {
            return Response.success(List.of());
        }

        // 只展示仍然已发布的内容：热门 ZSet 里可能残留已下架/已删除的 id
        Map<Long, ContentDO> contents = contentMapper.selectBatchIds(ids).stream()
                .filter(c -> "PUBLISHED".equals(c.getStatus()))
                .collect(Collectors.toMap(ContentDO::getId, c -> c, (a, b) -> a));

        Map<Long, String> categoryNames = categoryNames(
                contents.values().stream().map(ContentDO::getCategoryId).collect(Collectors.toSet()));

        // 按 ZSet 的顺序输出，而不是按 selectBatchIds 返回的顺序
        List<ContentListVO> list = ids.stream()
                .map(contents::get)
                .filter(Objects::nonNull)
                .map(c -> toListVO(c, categoryNames.get(c.getCategoryId())))
                .toList();

        return Response.success(list);
    }

    @Override
    public Response<PageResponse<ContentListVO>> pageMine(ContentPageReqVO req) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();
        return Response.success(doPage(req, loginUser.getUserId(), null));
    }

    @Override
    public Response<PageResponse<ContentListVO>> pageForReview(ContentPageReqVO req) {
        String status = StringUtils.isNotBlank(req.getStatus()) ? req.getStatus() : "PENDING";
        return Response.success(doPage(req, null, status, true));
    }

    @Override
    public Response<ContentDetailVO> findPublishedById(Long id) {
        // 阶段 5 Day 39：先查 Redis 缓存，未命中再打库并回填
        ContentDO content = cacheService.get(id);
        if (content == null) {
            content = contentMapper.selectById(id);
            if (content != null) {
                cacheService.put(content);
            }
        }

        if (Objects.isNull(content) || !"PUBLISHED".equals(content.getStatus())) {
            throw new BizException(ResponseCodeEnum.CONTENT_NOT_FOUND);
        }

        // Day 42：浏览量 INCR（写 Redis，定时任务再落库）
        statService.recordView(id);
        // Day 46：记录阅读历史（未登录时静默跳过）
        readingHistoryService.record(id, null);

        LoginUser loginUser = CurrentUserUtil.getLoginUser();
        ContentAccessService.AccessDecision decision = accessService.decide(content, loginUser);

        // 库里的 viewCount 是「上次同步时的值」，把 Redis 里待同步的增量加上，
        // 否则刚访问完刷新页面看不到浏览量变化
        int effectiveViews = (content.getViewCount() == null ? 0 : content.getViewCount())
                + (int) Math.min(Integer.MAX_VALUE, statService.pendingViews(id));

        ContentDetailVO vo = toDetailVO(content, categoryName(content.getCategoryId()), loginUser, decision);
        vo.setViewCount(effectiveViews);
        return Response.success(vo);
    }

    @Override
    public Response<ContentDetailVO> findMineById(Long id) {
        ContentDO content = cacheService.get(id);
        if (content == null) {
            content = contentMapper.selectById(id);
        }
        if (Objects.isNull(content)) {
            throw new BizException(ResponseCodeEnum.CONTENT_NOT_FOUND);
        }
        requireOwnership(content);

        LoginUser loginUser = CurrentUserUtil.getLoginUser();
        // 作者自己看自己的内容（含草稿）始终是全文
        return Response.success(toDetailVO(content, categoryName(content.getCategoryId()), loginUser,
                ContentAccessService.AccessDecision.allow()));
    }

    // ------------------------------------------------------------------ 写入

    @Override
    @Transactional
    public Response<Long> create(ContentReqVO req) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();
        if (!"CREATOR".equals(loginUser.getRole()) && !"ADMIN".equals(loginUser.getRole())) {
            throw new BizException(ResponseCodeEnum.NOT_CREATOR);
        }
        validateCategory(req.getCategoryId());

        ContentDO content = ContentDO.builder()
                .creatorId(loginUser.getUserId())
                .categoryId(req.getCategoryId())
                .title(req.getTitle().trim())
                .summary(req.getSummary())
                .cover(req.getCover())
                .contentType(req.getContentType().trim().toUpperCase())
                .body(req.getBody())
                .fileUrl(req.getFileUrl())
                .accessType(StringUtils.defaultIfBlank(req.getAccessType(), "FREE").toUpperCase())
                // 阶段 3 起引入审核：新建一律是草稿，不能直接自发布
                .status("DRAFT")
                .viewCount(0)
                .likeCount(0)
                .build();

        contentMapper.insert(content);
        return Response.success(content.getId());
    }

    @Override
    @Transactional
    public Response<Void> update(Long id, ContentReqVO req) {
        ContentDO existing = requireExisting(id);
        requireOwnership(existing);
        validateCategory(req.getCategoryId());

        // 编辑接口刻意「不碰状态」：状态只能通过 submit / offline / approve / reject 流转。
        // 早期版本允许在这里传 DRAFT/OFFLINE，结果是「编辑一篇已发布文章时，
        // 前端把 PUBLISHED 回显成 DRAFT 再提交」会把它悄悄降级成草稿。
        ContentDO update = ContentDO.builder()
                .id(id)
                .categoryId(req.getCategoryId())
                .title(req.getTitle().trim())
                .summary(req.getSummary())
                .cover(req.getCover())
                .contentType(req.getContentType().trim().toUpperCase())
                .body(req.getBody())
                .fileUrl(req.getFileUrl())
                .accessType(StringUtils.defaultIfBlank(req.getAccessType(), existing.getAccessType()).toUpperCase())
                .build();

        contentMapper.updateById(update);
        // Day 40：改完数据库必须清缓存，否则读到的还是旧内容
        cacheService.evict(id);
        return Response.success();
    }

    @Override
    @Transactional
    public Response<Void> delete(Long id) {
        ContentDO existing = requireExisting(id);
        requireOwnership(existing);
        contentMapper.deleteById(id);
        cacheService.evict(id);
        return Response.success();
    }

    // ------------------------------------------------------------------ 状态流转

    @Override
    @Transactional
    public Response<Void> submit(Long id) {
        ContentDO content = requireExisting(id);
        requireOwnership(content);
        return transition(content, "PENDING", null);
    }

    @Override
    @Transactional
    public Response<Void> offline(Long id) {
        ContentDO content = requireExisting(id);
        requireOwnership(content);
        return transition(content, "OFFLINE", null);
    }

    @Override
    @Transactional
    public Response<Void> approve(Long id) {
        ContentDO content = requireExisting(id);
        Response<Void> result = transition(content, "PUBLISHED", null);

        // 审核结果主动告知作者：否则他只能自己去工作台翻状态
        notificationService.push(content.getCreatorId(), "CONTENT_APPROVED", "内容已通过审核",
                "《" + content.getTitle() + "》已发布，现在可以在内容库看到了。",
                "CONTENT", content.getId());

        return result;
    }

    @Override
    @Transactional
    public Response<Void> reject(Long id, String reason) {
        ContentDO content = requireExisting(id);
        Response<Void> result = transition(content, "REJECTED", reason);

        notificationService.push(content.getCreatorId(), "CONTENT_REJECTED", "内容未通过审核",
                "《" + content.getTitle() + "》被驳回：" +
                        (StringUtils.isBlank(reason) ? "管理员未填写原因" : reason),
                "CONTENT", content.getId());

        return result;
    }

    /**
     * 统一的状态流转入口：先查状态机是否允许，再更新。
     *
     * <p>状态一变，内容就有资格/失去出现在公开页，缓存必须同步失效。</p>
     */
    private Response<Void> transition(ContentDO content, String target, String rejectReason) {
        String current = content.getStatus();
        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());

        if (!allowed.contains(target)) {
            throw new BizException(ResponseCodeEnum.CONTENT_STATUS_ILLEGAL.getErrorCode(),
                    String.format("状态不允许从 %s 变更为 %s", current, target));
        }

        ContentDO update = ContentDO.builder()
                .id(content.getId())
                .status(target)
                .rejectReason("REJECTED".equals(target) ? rejectReason : null)
                .build();

        contentMapper.updateById(update);

        if (!"REJECTED".equals(target)) {
            // updateById 会忽略 null 字段，所以「清空驳回原因」必须走显式 UPDATE
            contentMapper.clearRejectReason(content.getId());
        }

        cacheService.evict(content.getId());
        log.info("内容 {} 状态 {} -> {}", content.getId(), current, target);
        return Response.success();
    }

    // ------------------------------------------------------------------ 内部方法

    private PageResponse<ContentListVO> doPage(ContentPageReqVO req, Long creatorId, String forcedStatus) {
        return doPage(req, creatorId, forcedStatus, false);
    }

    private PageResponse<ContentListVO> doPage(ContentPageReqVO req, Long creatorId,
                                               String forcedStatus, boolean statusOptional) {
        LambdaQueryWrapper<ContentDO> query = new LambdaQueryWrapper<>();

        if (creatorId != null) {
            // 「我的内容」：强制锁定为当前登录用户，忽略请求里传的 creatorId
            query.eq(ContentDO::getCreatorId, creatorId);
        } else if (req.getCreatorId() != null) {
            // 创作者公开主页：按创作者筛选，属于公开的查询条件
            query.eq(ContentDO::getCreatorId, req.getCreatorId());
        }
        if (StringUtils.isNotBlank(forcedStatus)) {
            query.eq(ContentDO::getStatus, forcedStatus.trim().toUpperCase());
        } else if (statusOptional && StringUtils.isNotBlank(req.getStatus())) {
            query.eq(ContentDO::getStatus, req.getStatus().trim().toUpperCase());
        }

        if (req.getCategoryId() != null) {
            query.eq(ContentDO::getCategoryId, req.getCategoryId());
        }
        if (StringUtils.isNotBlank(req.getContentType())) {
            query.eq(ContentDO::getContentType, req.getContentType().trim().toUpperCase());
        }
        if (StringUtils.isNotBlank(req.getKeyword())) {
            String keyword = req.getKeyword().trim();
            query.and(w -> w.like(ContentDO::getTitle, keyword).or().like(ContentDO::getSummary, keyword));
        }

        query.orderByDesc(ContentDO::getCreateTime).orderByDesc(ContentDO::getId);

        Page<ContentDO> page = contentMapper.selectPage(Page.of(req.getPageNum(), req.getPageSize()), query);

        Map<Long, String> categoryNames = categoryNames(
                page.getRecords().stream().map(ContentDO::getCategoryId).collect(Collectors.toSet()));

        return PageResponse.of(page, content -> toListVO(content, categoryNames.get(content.getCategoryId())));
    }

    private ContentDO requireExisting(Long id) {
        ContentDO content = contentMapper.selectById(id);
        if (Objects.isNull(content)) {
            throw new BizException(ResponseCodeEnum.CONTENT_NOT_FOUND);
        }
        return content;
    }

    private void requireOwnership(ContentDO content) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();
        if ("ADMIN".equals(loginUser.getRole())) {
            return;
        }
        if (!Objects.equals(content.getCreatorId(), loginUser.getUserId())) {
            throw new BizException(ResponseCodeEnum.NOT_CONTENT_OWNER);
        }
    }

    private void validateCategory(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        if (Objects.isNull(categoryMapper.selectById(categoryId))) {
            throw new BizException(ResponseCodeEnum.CATEGORY_NOT_FOUND);
        }
    }

    private String categoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        ContentCategoryDO category = categoryMapper.selectById(categoryId);
        return category == null ? null : category.getName();
    }

    private Map<Long, String> categoryNames(Set<Long> categoryIds) {
        List<Long> ids = categoryIds.stream().filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(ContentCategoryDO::getId, ContentCategoryDO::getName, (a, b) -> a));
    }

    /** 试读片段：截断正文，正文为空时退回摘要 */
    private String buildPreview(ContentDO content) {
        String source = StringUtils.isNotBlank(content.getBody()) ? content.getBody() : content.getSummary();
        if (StringUtils.isBlank(source)) {
            return null;
        }
        String plain = source.replaceAll("[#*`>\\-]", "").trim();
        if (plain.length() <= ContentAccessService.PREVIEW_LENGTH) {
            return plain;
        }
        return plain.substring(0, ContentAccessService.PREVIEW_LENGTH) + "……";
    }

    private ContentListVO toListVO(ContentDO c, String categoryName) {
        return ContentListVO.builder()
                .id(c.getId())
                .creatorId(c.getCreatorId())
                .categoryId(c.getCategoryId())
                .categoryName(categoryName)
                .title(c.getTitle())
                .summary(c.getSummary())
                .cover(c.getCover())
                .contentType(c.getContentType())
                .accessType(c.getAccessType())
                .status(c.getStatus())
                .rejectReason(c.getRejectReason())
                .viewCount(c.getViewCount())
                .likeCount(c.getLikeCount())
                .createTime(c.getCreateTime())
                .updateTime(c.getUpdateTime())
                .build();
    }

    private ContentDetailVO toDetailVO(ContentDO c, String categoryName, LoginUser loginUser,
                                       ContentAccessService.AccessDecision decision) {
        boolean locked = !decision.full();
        long favoriteCount = favoriteService.countByContent(c.getId());
        boolean favorited = loginUser != null && favoriteService.isFavorited(loginUser.getUserId(), c.getId());

        return ContentDetailVO.builder()
                .id(c.getId())
                .creatorId(c.getCreatorId())
                .categoryId(c.getCategoryId())
                .categoryName(categoryName)
                .title(c.getTitle())
                .summary(c.getSummary())
                .cover(c.getCover())
                .contentType(c.getContentType())
                // 无权限时正文与附件地址都不下发，避免前端一抓就有
                .body(locked ? null : c.getBody())
                .bodyPreview(locked ? buildPreview(c) : null)
                .locked(locked)
                .lockReason(locked ? decision.lockReason() : null)
                .fileUrl(locked ? null : c.getFileUrl())
                .accessType(c.getAccessType())
                .status(c.getStatus())
                .rejectReason(c.getRejectReason())
                .viewCount(c.getViewCount())
                .likeCount(c.getLikeCount())
                .createTime(c.getCreateTime())
                .updateTime(c.getUpdateTime())
                .favorited(favorited)
                .favoriteCount(favoriteCount)
                .commentCount(commentService.countByContent(c.getId()))
                .hotScore(statService.hotScore(c.getId()))
                .build();
    }
}
