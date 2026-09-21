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
import com.contenthub.web.service.ContentService;
import com.contenthub.web.util.CurrentUserUtil;
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
public class ContentServiceImpl implements ContentService {

    private final ContentMapper contentMapper;
    private final ContentCategoryMapper categoryMapper;

    public ContentServiceImpl(ContentMapper contentMapper, ContentCategoryMapper categoryMapper) {
        this.contentMapper = contentMapper;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public Response<PageResponse<ContentListVO>> pagePublished(ContentPageReqVO req) {
        return Response.success(doPage(req, null, true));
    }

    @Override
    public Response<PageResponse<ContentListVO>> pageMine(ContentPageReqVO req) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();
        return Response.success(doPage(req, loginUser.getUserId(), false));
    }

    @Override
    public Response<ContentDetailVO> findPublishedById(Long id) {
        ContentDO content = contentMapper.selectById(id);
        if (Objects.isNull(content) || !"PUBLISHED".equals(content.getStatus())) {
            throw new BizException(ResponseCodeEnum.CONTENT_NOT_FOUND);
        }
        return Response.success(toDetailVO(content, categoryName(content.getCategoryId())));
    }

    @Override
    public Response<ContentDetailVO> findMineById(Long id) {
        ContentDO content = requireExisting(id);
        requireOwnership(content);
        return Response.success(toDetailVO(content, categoryName(content.getCategoryId())));
    }

    @Override
    @Transactional
    public Response<Long> create(ContentReqVO req) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();
        validateCategory(req.getCategoryId());
        validateStatus(req.getStatus());

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
                .status(StringUtils.defaultIfBlank(req.getStatus(), "DRAFT").toUpperCase())
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
        validateStatus(req.getStatus());

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
                .status(StringUtils.defaultIfBlank(req.getStatus(), existing.getStatus()).toUpperCase())
                .build();

        contentMapper.updateById(update);
        return Response.success();
    }

    @Override
    @Transactional
    public Response<Void> delete(Long id) {
        ContentDO existing = requireExisting(id);
        requireOwnership(existing);

        // 有 @TableLogic，这里实际执行的是 UPDATE is_deleted = 1
        contentMapper.deleteById(id);
        return Response.success();
    }

    // ------------------------------------------------------------------ 内部方法

    /**
     * 统一的分页查询。
     *
     * @param creatorId    不为空时只看该创作者的内容
     * @param onlyPublished true 时强制只看已发布（公开接口）
     */
    private PageResponse<ContentListVO> doPage(ContentPageReqVO req, Long creatorId, boolean onlyPublished) {
        LambdaQueryWrapper<ContentDO> query = new LambdaQueryWrapper<>();

        if (onlyPublished) {
            query.eq(ContentDO::getStatus, "PUBLISHED");
        } else {
            query.eq(ContentDO::getCreatorId, creatorId);
            if (StringUtils.isNotBlank(req.getStatus())) {
                query.eq(ContentDO::getStatus, req.getStatus().trim().toUpperCase());
            }
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

        // 分页插件已在 MybatisPlusConfig 中装配，这里才是真正的 LIMIT 查询
        Page<ContentDO> page = contentMapper.selectPage(
                Page.of(req.getPageNum(), req.getPageSize()), query);

        Map<Long, String> categoryNames = categoryNames(
                page.getRecords().stream().map(ContentDO::getCategoryId).collect(Collectors.toSet()));

        return PageResponse.of(page, content -> toListVO(content, categoryNames.get(content.getCategoryId())));
    }

    /** 内容必须存在（已被逻辑删除的查不到） */
    private ContentDO requireExisting(Long id) {
        ContentDO content = contentMapper.selectById(id);
        if (Objects.isNull(content)) {
            throw new BizException(ResponseCodeEnum.CONTENT_NOT_FOUND);
        }
        return content;
    }

    /**
     * 归属校验：创作者只能改自己的内容，管理员不受限。
     *
     * <p>计划里这条属于阶段 3 Day 22，但既然 Security 只按角色放行，
     * 不做归属校验就等于任何创作者都能改别人的内容，所以提前落地。</p>
     */
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

    /** 阶段 1 只允许这三个状态；PENDING / REJECTED 属于阶段 3 的审核流转 */
    private void validateStatus(String status) {
        if (StringUtils.isBlank(status)) {
            return;
        }
        String normalized = status.trim().toUpperCase();
        if (!Set.of("DRAFT", "PUBLISHED", "OFFLINE").contains(normalized)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode(),
                    "状态只能是 DRAFT / PUBLISHED / OFFLINE（审核流转属于阶段 3）");
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
                .viewCount(c.getViewCount())
                .likeCount(c.getLikeCount())
                .createTime(c.getCreateTime())
                .updateTime(c.getUpdateTime())
                .build();
    }

    private ContentDetailVO toDetailVO(ContentDO c, String categoryName) {
        return ContentDetailVO.builder()
                .id(c.getId())
                .creatorId(c.getCreatorId())
                .categoryId(c.getCategoryId())
                .categoryName(categoryName)
                .title(c.getTitle())
                .summary(c.getSummary())
                .cover(c.getCover())
                .contentType(c.getContentType())
                .body(c.getBody())
                .fileUrl(c.getFileUrl())
                .accessType(c.getAccessType())
                .status(c.getStatus())
                .viewCount(c.getViewCount())
                .likeCount(c.getLikeCount())
                .createTime(c.getCreateTime())
                .updateTime(c.getUpdateTime())
                .build();
    }
}
