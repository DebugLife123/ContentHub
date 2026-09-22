package com.contenthub.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.contenthub.common.domain.dos.SkillCategoryDO;
import com.contenthub.common.domain.dos.SkillDO;
import com.contenthub.common.domain.mapper.SkillCategoryMapper;
import com.contenthub.common.domain.mapper.SkillMapper;
import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.common.utils.JsonUtil;
import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.jwt.model.LoginUser;
import com.contenthub.web.model.req.SkillPageReqVO;
import com.contenthub.web.model.req.SkillReqVO;
import com.contenthub.web.model.vo.SkillDetailVO;
import com.contenthub.web.model.vo.SkillListVO;
import com.contenthub.web.model.vo.SkillStepVO;
import com.contenthub.web.model.vo.SkillTeamVO;
import com.contenthub.web.service.SkillService;
import com.contenthub.web.service.SubscriptionService;
import com.contenthub.web.util.CurrentUserUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Skill 商城服务实现。
 *
 * <p>Skill 由管理员维护，所以这里没有「作者」概念：编辑与删除不校验归属，
 * 权限完全由 {@code WebSecurityConfig} 的 {@code /admin/**} 规则把关。</p>
 */
@Service
public class SkillServiceImpl implements SkillService {

    private static final String DRAFT = "DRAFT";
    private static final String PUBLISHED = "PUBLISHED";
    private static final String OFFLINE = "OFFLINE";
    private static final String ACCESS_MEMBER = "MEMBER";

    /** 允许的状态流转；没有列出的组合一律拒绝 */
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            DRAFT, Set.of(PUBLISHED),
            OFFLINE, Set.of(PUBLISHED),
            PUBLISHED, Set.of(OFFLINE)
    );

    private final SkillMapper skillMapper;
    private final SkillCategoryMapper categoryMapper;
    private final SubscriptionService subscriptionService;

    public SkillServiceImpl(SkillMapper skillMapper,
                            SkillCategoryMapper categoryMapper,
                            SubscriptionService subscriptionService) {
        this.skillMapper = skillMapper;
        this.categoryMapper = categoryMapper;
        this.subscriptionService = subscriptionService;
    }

    // ------------------------------------------------------------------ 公开读

    @Override
    public Response<PageResponse<SkillListVO>> pagePublished(SkillPageReqVO req) {
        // 公开接口强制只看已上架，忽略请求里可能带的 status，避免被人翻出草稿
        return Response.success(doPage(req, PUBLISHED));
    }

    @Override
    public Response<SkillDetailVO> findPublishedById(Long id) {
        SkillDO skill = requireExisting(id);
        if (!PUBLISHED.equals(skill.getStatus())) {
            throw new BizException(ResponseCodeEnum.SKILL_NOT_FOUND);
        }
        return Response.success(toDetail(skill, categoryName(skill.getCategoryId()), decideAccess(skill)));
    }

    // ------------------------------------------------------------------ 管理端

    @Override
    public Response<PageResponse<SkillListVO>> pageForAdmin(SkillPageReqVO req) {
        return Response.success(doPage(req, req.getStatus()));
    }

    @Override
    public Response<SkillDetailVO> findByIdForAdmin(Long id) {
        SkillDO skill = requireExisting(id);
        // 管理员不受会员限制，草稿与已下架也照常返回完整内容
        return Response.success(toDetail(skill, categoryName(skill.getCategoryId()), Access.allow()));
    }

    @Override
    @Transactional
    public Response<Long> create(SkillReqVO req) {
        String name = req.getName().trim();
        validateCategory(req.getCategoryId());

        // uk_skill_name 不区分逻辑删除，所以要先看「含已删除」的同名行：
        // 命中在用的 -> 报重名；命中已删除的 -> 复活它再覆盖成新数据
        SkillDO existing = skillMapper.findByNameIncludingDeleted(name);
        if (existing != null) {
            if (!Boolean.TRUE.equals(existing.getDeleted())) {
                throw new BizException(ResponseCodeEnum.SKILL_NAME_EXISTS);
            }
            skillMapper.revive(existing.getId());
            skillMapper.update(null, fullUpdate(existing.getId(), req, name));
            return Response.success(existing.getId());
        }

        SkillDO skill = SkillDO.builder()
                .name(name)
                .accessType(StringUtils.isBlank(req.getAccessType()) ? "FREE" : req.getAccessType())
                .status(DRAFT)
                .build();
        applyEditableFields(skill, req);
        skillMapper.insert(skill);

        return Response.success(skill.getId());
    }

    @Override
    @Transactional
    public Response<Void> update(Long id, SkillReqVO req) {
        requireExisting(id);
        String name = req.getName().trim();
        validateCategory(req.getCategoryId());

        SkillDO sameName = skillMapper.findByNameIncludingDeleted(name);
        if (sameName != null && !Objects.equals(sameName.getId(), id)) {
            throw new BizException(ResponseCodeEnum.SKILL_NAME_EXISTS);
        }

        skillMapper.update(null, fullUpdate(id, req, name));
        return Response.success();
    }

    @Override
    @Transactional
    public Response<Void> delete(Long id) {
        requireExisting(id);
        skillMapper.deleteById(id);
        return Response.success();
    }

    @Override
    @Transactional
    public Response<Void> publish(Long id) {
        return transition(id, PUBLISHED);
    }

    @Override
    @Transactional
    public Response<Void> offline(Long id) {
        return transition(id, OFFLINE);
    }

    // ------------------------------------------------------------------ 内部

    private Response<Void> transition(Long id, String target) {
        SkillDO skill = requireExisting(id);
        if (!TRANSITIONS.getOrDefault(skill.getStatus(), Set.of()).contains(target)) {
            throw new BizException(ResponseCodeEnum.SKILL_STATUS_ILLEGAL);
        }
        // 只动 status，其余字段保持原样
        skillMapper.updateById(SkillDO.builder().id(id).status(target).build());
        return Response.success();
    }

    private SkillDO requireExisting(Long id) {
        SkillDO skill = skillMapper.selectById(id);
        if (Objects.isNull(skill)) {
            throw new BizException(ResponseCodeEnum.SKILL_NOT_FOUND);
        }
        return skill;
    }

    private void validateCategory(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        if (Objects.isNull(categoryMapper.selectById(categoryId))) {
            throw new BizException(ResponseCodeEnum.SKILL_CATEGORY_NOT_FOUND);
        }
    }

    /**
     * 编辑用的更新条件。
     *
     * <p>用 {@link LambdaUpdateWrapper} 逐列 {@code set} 而不是 {@code updateById(DO)}：
     * MyBatis-Plus 默认忽略 null 字段，而编辑表单是整体提交的——管理员把「摘要」清空时，
     * 用 DO 更新会把旧值悄悄留下。</p>
     */
    private LambdaUpdateWrapper<SkillDO> fullUpdate(Long id, SkillReqVO req, String name) {
        return new LambdaUpdateWrapper<SkillDO>()
                .eq(SkillDO::getId, id)
                .set(SkillDO::getName, name)
                .set(SkillDO::getIcon, req.getIcon())
                .set(SkillDO::getCategoryId, req.getCategoryId())
                .set(SkillDO::getSummary, req.getSummary())
                .set(SkillDO::getWhyIncluded, req.getWhyIncluded())
                .set(SkillDO::getAuthor, req.getAuthor())
                .set(SkillDO::getRepo, req.getRepo())
                .set(SkillDO::getOfficialUrl, req.getOfficialUrl())
                .set(SkillDO::getInstallCommand, req.getInstallCommand())
                .set(SkillDO::getStars, req.getStars() == null ? 0 : req.getStars())
                .set(SkillDO::getVersion, req.getVersion())
                .set(SkillDO::getLicense, req.getLicense())
                .set(SkillDO::getSize, req.getSize())
                .set(SkillDO::getDownloads, req.getDownloads() == null ? 0 : req.getDownloads())
                .set(SkillDO::getSecurityLevel, req.getSecurityLevel() == null ? 0 : req.getSecurityLevel())
                .set(SkillDO::getSecurityLabel, req.getSecurityLabel())
                .set(SkillDO::getSubmitter, req.getSubmitter())
                .set(SkillDO::getSubmitTime, req.getSubmitTime())
                .set(SkillDO::getPlatforms, joinComma(req.getPlatforms()))
                .set(SkillDO::getTags, joinComma(req.getTags()))
                .set(SkillDO::getFeatures, joinLines(req.getFeatures()))
                .set(SkillDO::getQuickStart, JsonUtil.toJsonString(
                        req.getQuickStart() == null ? List.of() : req.getQuickStart()))
                .set(SkillDO::getTeamMaintainers, req.getTeamMaintainers() == null ? 0 : req.getTeamMaintainers())
                .set(SkillDO::getTeamContributors, req.getTeamContributors() == null ? 0 : req.getTeamContributors())
                .set(SkillDO::getTeamOpenIssues, req.getTeamOpenIssues() == null ? 0 : req.getTeamOpenIssues())
                .set(SkillDO::getTeamLastCommit, req.getTeamLastCommit())
                .set(SkillDO::getAccessType,
                        StringUtils.isBlank(req.getAccessType()) ? "FREE" : req.getAccessType());
    }

    /** 新增时把可编辑字段灌进 DO；status 与 name 由调用方决定，不在这里覆盖 */
    private void applyEditableFields(SkillDO target, SkillReqVO req) {
        target.setIcon(req.getIcon());
        target.setCategoryId(req.getCategoryId());
        target.setSummary(req.getSummary());
        target.setWhyIncluded(req.getWhyIncluded());
        target.setAuthor(req.getAuthor());
        target.setRepo(req.getRepo());
        target.setOfficialUrl(req.getOfficialUrl());
        target.setInstallCommand(req.getInstallCommand());
        target.setStars(req.getStars() == null ? 0 : req.getStars());
        target.setVersion(req.getVersion());
        target.setLicense(req.getLicense());
        target.setSize(req.getSize());
        target.setDownloads(req.getDownloads() == null ? 0 : req.getDownloads());
        target.setSecurityLevel(req.getSecurityLevel() == null ? 0 : req.getSecurityLevel());
        target.setSecurityLabel(req.getSecurityLabel());
        target.setSubmitter(req.getSubmitter());
        target.setSubmitTime(req.getSubmitTime());
        target.setPlatforms(joinComma(req.getPlatforms()));
        target.setTags(joinComma(req.getTags()));
        target.setFeatures(joinLines(req.getFeatures()));
        target.setQuickStart(JsonUtil.toJsonString(
                req.getQuickStart() == null ? List.of() : req.getQuickStart()));
        target.setTeamMaintainers(req.getTeamMaintainers() == null ? 0 : req.getTeamMaintainers());
        target.setTeamContributors(req.getTeamContributors() == null ? 0 : req.getTeamContributors());
        target.setTeamOpenIssues(req.getTeamOpenIssues() == null ? 0 : req.getTeamOpenIssues());
        target.setTeamLastCommit(req.getTeamLastCommit());
    }

    private PageResponse<SkillListVO> doPage(SkillPageReqVO req, String status) {
        LambdaQueryWrapper<SkillDO> query = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(status)) {
            query.eq(SkillDO::getStatus, status);
        }
        if (req.getCategoryId() != null) {
            query.eq(SkillDO::getCategoryId, req.getCategoryId());
        }
        String keyword = req.getKeyword() == null ? null : req.getKeyword().trim();
        if (StringUtils.isNotBlank(keyword)) {
            query.and(w -> w.like(SkillDO::getName, keyword)
                    .or().like(SkillDO::getSummary, keyword)
                    .or().like(SkillDO::getAuthor, keyword)
                    .or().like(SkillDO::getRepo, keyword)
                    .or().like(SkillDO::getTags, keyword));
        }
        applySort(query, req.getSort());

        Page<SkillDO> page = skillMapper.selectPage(new Page<>(req.getPageNum(), req.getPageSize()), query);
        Map<Long, String> names = categoryNames(page.getRecords());
        return PageResponse.of(page, s -> toList(s, names));
    }

    /** 默认按星数倒序——商城的核心排序依据 */
    private void applySort(LambdaQueryWrapper<SkillDO> query, String sort) {
        String s = StringUtils.isBlank(sort) ? "stars" : sort;
        switch (s) {
            case "updated" -> query.orderByDesc(SkillDO::getUpdateTime).orderByDesc(SkillDO::getId);
            case "name" -> query.orderByAsc(SkillDO::getName);
            // 星数相同时用名称兜底，保证翻页顺序稳定
            default -> query.orderByDesc(SkillDO::getStars).orderByAsc(SkillDO::getName);
        }
    }

    private SkillListVO toList(SkillDO skill, Map<Long, String> categoryNames) {
        return SkillListVO.builder()
                .id(skill.getId())
                .name(skill.getName())
                .icon(skill.getIcon())
                .categoryId(skill.getCategoryId())
                .categoryName(skill.getCategoryId() == null ? null : categoryNames.get(skill.getCategoryId()))
                .summary(skill.getSummary())
                .author(skill.getAuthor())
                .repo(skill.getRepo())
                .stars(skill.getStars())
                .version(skill.getVersion())
                .accessType(skill.getAccessType())
                .status(skill.getStatus())
                .platforms(splitComma(skill.getPlatforms()))
                .tags(splitComma(skill.getTags()))
                .updateTime(skill.getUpdateTime())
                .build();
    }

    private SkillDetailVO toDetail(SkillDO skill, String categoryName, Access access) {
        boolean locked = !access.full();
        return SkillDetailVO.builder()
                .id(skill.getId())
                .name(skill.getName())
                .icon(skill.getIcon())
                .categoryId(skill.getCategoryId())
                .categoryName(categoryName)
                .summary(skill.getSummary())
                .author(skill.getAuthor())
                .repo(skill.getRepo())
                .officialUrl(skill.getOfficialUrl())
                // 锁住时不下发安装命令与上手步骤，前端拿不到也就无从绕过
                .installCommand(locked ? "" : skill.getInstallCommand())
                .stars(skill.getStars())
                .version(skill.getVersion())
                .license(skill.getLicense())
                .size(skill.getSize())
                .downloads(skill.getDownloads())
                .securityLevel(skill.getSecurityLevel())
                .securityLabel(skill.getSecurityLabel())
                .submitter(skill.getSubmitter())
                .submitTime(skill.getSubmitTime())
                .platforms(splitComma(skill.getPlatforms()))
                .tags(splitComma(skill.getTags()))
                .features(splitLines(skill.getFeatures()))
                .whyIncluded(skill.getWhyIncluded())
                .quickStart(locked ? List.of() : JsonUtil.parseList(skill.getQuickStart(), SkillStepVO.class))
                .team(SkillTeamVO.builder()
                        .maintainers(skill.getTeamMaintainers())
                        .contributors(skill.getTeamContributors())
                        .openIssues(skill.getTeamOpenIssues())
                        .lastCommit(skill.getTeamLastCommit())
                        .build())
                .accessType(skill.getAccessType())
                .status(skill.getStatus())
                .locked(locked)
                .lockReason(locked ? access.reason() : null)
                .updateTime(skill.getUpdateTime())
                .build();
    }

    /**
     * 会员判定。
     *
     * <p>Skill 没有「作者」概念，所以不像内容库那样按创作者判权限，
     * 而是「持有任意一条有效订阅即为会员」。</p>
     */
    private Access decideAccess(SkillDO skill) {
        if (!ACCESS_MEMBER.equals(skill.getAccessType())) {
            return Access.allow();
        }
        LoginUser loginUser = CurrentUserUtil.getLoginUser();
        if (loginUser == null) {
            return Access.deny("这个 Skill 需要会员解锁，登录并订阅后可查看完整说明与安装方式");
        }
        if ("ADMIN".equals(loginUser.getRole())
                || subscriptionService.hasAnyActiveSubscription(loginUser.getUserId())) {
            return Access.allow();
        }
        return Access.deny("这个 Skill 需要会员解锁，订阅后即可查看完整说明与安装方式");
    }

    private String categoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        SkillCategoryDO category = categoryMapper.selectById(categoryId);
        return category == null ? null : category.getName();
    }

    /** 一次查出本页涉及的所有分类名，避免逐行查库 */
    private Map<Long, String> categoryNames(List<SkillDO> skills) {
        Set<Long> ids = skills.stream()
                .map(SkillDO::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> names = new HashMap<>();
        for (SkillCategoryDO category : categoryMapper.selectBatchIds(ids)) {
            names.put(category.getId(), category.getName());
        }
        return names;
    }

    // ---- 文本列与列表的互转（与 SkillDO 的字段说明对应）

    private List<String> splitComma(String value) {
        if (StringUtils.isBlank(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String joinComma(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
    }

    private List<String> splitLines(String value) {
        if (StringUtils.isBlank(value)) {
            return List.of();
        }
        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String joinLines(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n"));
    }

    /** 是否可以看到完整内容 */
    private record Access(boolean full, String reason) {
        static Access allow() {
            return new Access(true, null);
        }

        static Access deny(String reason) {
            return new Access(false, reason);
        }
    }
}
