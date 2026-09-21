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
import com.contenthub.web.service.ContentAccessService;
import com.contenthub.web.service.ContentService;
import com.contenthub.web.service.FavoriteService;
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

    public ContentServiceImpl(ContentMapper contentMapper,
                              ContentCategoryMapper categoryMapper,
                              ContentAccessService accessService,
                              FavoriteService favoriteService) {
        this.contentMapper = contentMapper;
        this.categoryMapper = categoryMapper;
        this.accessService = accessService;
        this.favoriteService = favoriteService;
    }

    // ------------------------------------------------------------------ 查询

    @Override
    public Response<PageResponse<ContentListVO>> pagePublished(ContentPageReqVO req) {
        return Response.success(doPage(req, null, "PUBLISHED"));
    }

    @Override
    public Response<PageResponse<ContentListVO>> pageMine(ContentPageReqVO req) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();
        return Response.success(doPage(req, loginUser.getUserId(), null));
    }

    @Override
    public Response<PageResponse<ContentListVO>> pageForReview(ContentPageReqVO req) {
        // 管理端看全部内容；不传 status 时默认聚焦待审核，方便审核页打开即用
        String status = StringUtils.isNotBlank(req.getStatus()) ? req.getStatus() : "PENDING";
        return Response.success(doPage(req, null, status, true));
    }

    @Override
    public Response<ContentDetailVO> findPublishedById(Long id) {
        ContentDO content = contentMapper.selectById(id);
        if (Objects.isNull(content) || !"PUBLISHED".equals(content.getStatus())) {
            throw new BizException(ResponseCodeEnum.CONTENT_NOT_FOUND);
        }

        LoginUser loginUser = CurrentUserUtil.getLoginUser();
        // 计划 Day 25-27：免费内容直接给全文，订阅内容按权限决定是否只给试读
        ContentAccessService.AccessDecision decision = accessService.decide(content, loginUser);

        return Response.success(toDetailVO(content, categoryName(content.getCategoryId()), loginUser, decision));
    }

    @Override
    public Response<ContentDetailVO> findMineById(Long id) {
        ContentDO content = requireExisting(id);
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

        String status = existing.getStatus();
        if (StringUtils.isNotBlank(req.getStatus())) {
            String requested = req.getStatus().trim().toUpperCase();
            if ("PUBLISHED".equals(requested)) {
                throw new BizException(ResponseCodeEnum.CONTENT_STATUS_ILLEGAL.getErrorCode(),
                        "内容需经管理员审核才能发布，请先提交审核");
            }
            if (!Set.of("DRAFT", "OFFLINE").contains(requested)) {
                throw new BizException(ResponseCodeEnum.CONTENT_STATUS_ILLEGAL.getErrorCode(),
                        "编辑时只能选择 DRAFT 或 OFFLINE，其余状态由审核流程控制");
            }
            status = requested;
        }

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
                .status(status)
                .build();

        contentMapper.updateById(update);
        return Response.success();
    }

    @Override
    @Transactional
    public Response<Void> delete(Long id) {
        ContentDO existing = requireExisting(id);
        requireOwnership(existing);
        contentMapper.deleteById(id);
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
        // 审核通过后清掉上一次的驳回原因
        return transition(content, "PUBLISHED", null);
    }

    @Override
    @Transactional
    public Response<Void> reject(Long id, String reason) {
        ContentDO content = requireExisting(id);
        return transition(content, "REJECTED", reason);
    }

    /**
     * 统一的状态流转入口：先查状态机是否允许，再更新。
     *
     * <p>把允许的流转写成表，避免各个接口各自 if 判断导致状态能被随意改。</p>
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
                // REJECTED 时写入驳回原因；其余状态传 null
                .rejectReason("REJECTED".equals(target) ? rejectReason : null)
                .build();

        contentMapper.updateById(update);

        if (!"REJECTED".equals(target)) {
            // updateById 会忽略 null 字段，所以「清空驳回原因」必须走显式 UPDATE，
            // 否则重新提交审核后还会残留上一次的驳回理由
            contentMapper.clearRejectReason(content.getId());
        }

        log.info("内容 {} 状态 {} -> {}", content.getId(), current, target);
        return Response.success();
    }

    // ------------------------------------------------------------------ 内部方法

    private PageResponse<ContentListVO> doPage(ContentPageReqVO req, Long creatorId, String forcedStatus) {
        return doPage(req, creatorId, forcedStatus, false);
    }

    /**
     * @param creatorId     不为空时只看该创作者的内容
     * @param forcedStatus  不为空时强制只看该状态
     * @param statusOptional forcedStatus 为空时是否仍允许按 req.status 过滤
     */
    private PageResponse<ContentListVO> doPage(ContentPageReqVO req, Long creatorId,
                                               String forcedStatus, boolean statusOptional) {
        LambdaQueryWrapper<ContentDO> query = new LambdaQueryWrapper<>();

        if (creatorId != null) {
            query.eq(ContentDO::getCreatorId, creatorId);
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

    /** 归属校验：创作者只能改自己的内容，管理员不受限 */
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
                .build();
    }
}
