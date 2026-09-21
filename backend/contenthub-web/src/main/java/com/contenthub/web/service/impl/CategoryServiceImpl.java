package com.contenthub.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.contenthub.common.domain.dos.ContentCategoryDO;
import com.contenthub.common.domain.dos.ContentDO;
import com.contenthub.common.domain.mapper.ContentCategoryMapper;
import com.contenthub.common.domain.mapper.ContentMapper;
import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.CategoryReqVO;
import com.contenthub.web.model.vo.CategoryVO;
import com.contenthub.web.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final ContentCategoryMapper categoryMapper;
    private final ContentMapper contentMapper;

    public CategoryServiceImpl(ContentCategoryMapper categoryMapper, ContentMapper contentMapper) {
        this.categoryMapper = categoryMapper;
        this.contentMapper = contentMapper;
    }

    @Override
    public Response<List<CategoryVO>> list(boolean enabledOnly) {
        LambdaQueryWrapper<ContentCategoryDO> query = new LambdaQueryWrapper<ContentCategoryDO>()
                .orderByAsc(ContentCategoryDO::getSort)
                .orderByAsc(ContentCategoryDO::getId);
        if (enabledOnly) {
            query.eq(ContentCategoryDO::getStatus, "ENABLED");
        }

        List<ContentCategoryDO> categories = categoryMapper.selectList(query);

        // 一次查出各分类下已发布内容数，避免 N+1
        Map<Long, Long> counts = countPublishedByCategory();

        List<CategoryVO> result = categories.stream()
                .map(c -> CategoryVO.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .sort(c.getSort())
                        .status(c.getStatus())
                        .contentCount(counts.getOrDefault(c.getId(), 0L))
                        .createTime(c.getCreateTime())
                        .build())
                .toList();

        return Response.success(result);
    }

    @Override
    @Transactional
    public Response<CategoryVO> create(CategoryReqVO req) {
        ensureNameAvailable(req.getName(), null);

        ContentCategoryDO category = ContentCategoryDO.builder()
                .name(req.getName().trim())
                .sort(req.getSort() == null ? 0 : req.getSort())
                .status(req.getStatus() == null ? "ENABLED" : req.getStatus())
                .build();

        categoryMapper.insert(category);

        return Response.success(CategoryVO.builder()
                .id(category.getId())
                .name(category.getName())
                .sort(category.getSort())
                .status(category.getStatus())
                .contentCount(0L)
                .build());
    }

    @Override
    @Transactional
    public Response<CategoryVO> update(Long id, CategoryReqVO req) {
        ContentCategoryDO existing = categoryMapper.selectById(id);
        if (Objects.isNull(existing)) {
            throw new BizException(ResponseCodeEnum.CATEGORY_NOT_FOUND);
        }
        ensureNameAvailable(req.getName(), id);

        ContentCategoryDO update = ContentCategoryDO.builder()
                .id(id)
                .name(req.getName().trim())
                .sort(req.getSort() == null ? existing.getSort() : req.getSort())
                .status(req.getStatus() == null ? existing.getStatus() : req.getStatus())
                .build();

        categoryMapper.updateById(update);

        return Response.success(CategoryVO.builder()
                .id(id)
                .name(update.getName())
                .sort(update.getSort())
                .status(update.getStatus())
                .build());
    }

    @Override
    @Transactional
    public Response<Void> delete(Long id) {
        ContentCategoryDO existing = categoryMapper.selectById(id);
        if (Objects.isNull(existing)) {
            throw new BizException(ResponseCodeEnum.CATEGORY_NOT_FOUND);
        }

        // 分类下有内容时不允许删除，否则内容会变成「无分类」的孤儿数据
        Long used = contentMapper.selectCount(new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getCategoryId, id));
        if (used != null && used > 0) {
            throw new BizException(ResponseCodeEnum.CATEGORY_IN_USE);
        }

        categoryMapper.deleteById(id);
        return Response.success();
    }

    /** 名称唯一性校验（数据库有 uk_name，这里提前给出友好提示） */
    private void ensureNameAvailable(String name, Long excludeId) {
        LambdaQueryWrapper<ContentCategoryDO> query = new LambdaQueryWrapper<ContentCategoryDO>()
                .eq(ContentCategoryDO::getName, name.trim());
        if (excludeId != null) {
            query.ne(ContentCategoryDO::getId, excludeId);
        }
        if (categoryMapper.selectCount(query) > 0) {
            throw new BizException(ResponseCodeEnum.CATEGORY_NAME_EXISTS);
        }
    }

    private Map<Long, Long> countPublishedByCategory() {
        List<ContentDO> published = contentMapper.selectList(new LambdaQueryWrapper<ContentDO>()
                .select(ContentDO::getCategoryId)
                .eq(ContentDO::getStatus, "PUBLISHED")
                .isNotNull(ContentDO::getCategoryId));

        Map<Long, Long> counts = new HashMap<>();
        for (ContentDO content : published) {
            counts.merge(content.getCategoryId(), 1L, Long::sum);
        }
        return counts;
    }
}
