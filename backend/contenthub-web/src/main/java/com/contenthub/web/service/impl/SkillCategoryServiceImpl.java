package com.contenthub.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.contenthub.common.domain.dos.SkillCategoryDO;
import com.contenthub.common.domain.dos.SkillDO;
import com.contenthub.common.domain.mapper.SkillCategoryMapper;
import com.contenthub.common.domain.mapper.SkillMapper;
import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.SkillCategoryReqVO;
import com.contenthub.web.model.vo.SkillCategoryVO;
import com.contenthub.web.service.SkillCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SkillCategoryServiceImpl implements SkillCategoryService {

    private final SkillCategoryMapper categoryMapper;
    private final SkillMapper skillMapper;

    public SkillCategoryServiceImpl(SkillCategoryMapper categoryMapper, SkillMapper skillMapper) {
        this.categoryMapper = categoryMapper;
        this.skillMapper = skillMapper;
    }

    @Override
    public Response<List<SkillCategoryVO>> list(boolean enabledOnly) {
        LambdaQueryWrapper<SkillCategoryDO> query = new LambdaQueryWrapper<SkillCategoryDO>()
                .orderByAsc(SkillCategoryDO::getSort)
                .orderByAsc(SkillCategoryDO::getId);
        if (enabledOnly) {
            query.eq(SkillCategoryDO::getStatus, "ENABLED");
        }

        List<SkillCategoryDO> categories = categoryMapper.selectList(query);

        // 一次查出各分类下的 Skill 数，避免 N+1
        Map<Long, Long> counts = countByCategory();

        List<SkillCategoryVO> result = categories.stream()
                .map(c -> SkillCategoryVO.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .sort(c.getSort())
                        .status(c.getStatus())
                        .skillCount(counts.getOrDefault(c.getId(), 0L))
                        .createTime(c.getCreateTime())
                        .build())
                .toList();

        return Response.success(result);
    }

    @Override
    @Transactional
    public Response<SkillCategoryVO> create(SkillCategoryReqVO req) {
        String name = req.getName().trim();
        int sort = req.getSort() == null ? 0 : req.getSort();
        String status = req.getStatus() == null ? "ENABLED" : req.getStatus();

        // uk_skill_category_name 不区分逻辑删除，命中已删除的同名行要复活而不是 INSERT
        SkillCategoryDO existing = categoryMapper.findByNameIncludingDeleted(name);
        if (existing != null) {
            if (!Boolean.TRUE.equals(existing.getDeleted())) {
                throw new BizException(ResponseCodeEnum.SKILL_CATEGORY_NAME_EXISTS);
            }
            categoryMapper.revive(existing.getId(), sort, status);
            return Response.success(SkillCategoryVO.builder()
                    .id(existing.getId())
                    .name(name)
                    .sort(sort)
                    .status(status)
                    .skillCount(0L)
                    .build());
        }

        SkillCategoryDO category = SkillCategoryDO.builder()
                .name(name)
                .sort(sort)
                .status(status)
                .build();
        categoryMapper.insert(category);

        return Response.success(SkillCategoryVO.builder()
                .id(category.getId())
                .name(category.getName())
                .sort(category.getSort())
                .status(category.getStatus())
                .skillCount(0L)
                .build());
    }

    @Override
    @Transactional
    public Response<SkillCategoryVO> update(Long id, SkillCategoryReqVO req) {
        SkillCategoryDO existing = categoryMapper.selectById(id);
        if (Objects.isNull(existing)) {
            throw new BizException(ResponseCodeEnum.SKILL_CATEGORY_NOT_FOUND);
        }

        // 改名撞上一条已删除记录时，只查未删除行会放过校验，随后 UPDATE 撞唯一键
        String name = req.getName().trim();
        SkillCategoryDO sameName = categoryMapper.findByNameIncludingDeleted(name);
        if (sameName != null && !Objects.equals(sameName.getId(), id)) {
            throw new BizException(ResponseCodeEnum.SKILL_CATEGORY_NAME_EXISTS);
        }

        SkillCategoryDO update = SkillCategoryDO.builder()
                .id(id)
                .name(name)
                .sort(req.getSort() == null ? existing.getSort() : req.getSort())
                .status(req.getStatus() == null ? existing.getStatus() : req.getStatus())
                .build();
        categoryMapper.updateById(update);

        return Response.success(SkillCategoryVO.builder()
                .id(id)
                .name(update.getName())
                .sort(update.getSort())
                .status(update.getStatus())
                .build());
    }

    @Override
    @Transactional
    public Response<Void> delete(Long id) {
        SkillCategoryDO existing = categoryMapper.selectById(id);
        if (Objects.isNull(existing)) {
            throw new BizException(ResponseCodeEnum.SKILL_CATEGORY_NOT_FOUND);
        }

        // 分类下还有 Skill 时不允许删除，否则那些 Skill 会变成「无分类」的孤儿数据
        Long used = skillMapper.selectCount(new LambdaQueryWrapper<SkillDO>()
                .eq(SkillDO::getCategoryId, id));
        if (used != null && used > 0) {
            throw new BizException(ResponseCodeEnum.SKILL_CATEGORY_IN_USE);
        }

        categoryMapper.deleteById(id);
        return Response.success();
    }

    private Map<Long, Long> countByCategory() {
        List<SkillDO> skills = skillMapper.selectList(new LambdaQueryWrapper<SkillDO>()
                .select(SkillDO::getCategoryId)
                .isNotNull(SkillDO::getCategoryId));

        Map<Long, Long> counts = new HashMap<>();
        for (SkillDO skill : skills) {
            counts.merge(skill.getCategoryId(), 1L, Long::sum);
        }
        return counts;
    }
}
