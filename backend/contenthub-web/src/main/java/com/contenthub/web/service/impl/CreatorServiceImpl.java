package com.contenthub.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.contenthub.common.domain.dos.CommentDO;
import com.contenthub.common.domain.dos.ContentDO;
import com.contenthub.common.domain.dos.CreatorProfileDO;
import com.contenthub.common.domain.dos.FavoriteDO;
import com.contenthub.common.domain.dos.SubscriptionDO;
import com.contenthub.common.domain.dos.SubscriptionPlanDO;
import com.contenthub.common.domain.dos.UserDO;
import com.contenthub.common.domain.mapper.CommentMapper;
import com.contenthub.common.domain.mapper.ContentMapper;
import com.contenthub.common.domain.mapper.CreatorProfileMapper;
import com.contenthub.common.domain.mapper.FavoriteMapper;
import com.contenthub.common.domain.mapper.SubscriptionMapper;
import com.contenthub.common.domain.mapper.SubscriptionPlanMapper;
import com.contenthub.common.domain.mapper.UserMapper;
import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.common.utils.Response;
import com.contenthub.jwt.model.LoginUser;
import com.contenthub.web.model.req.CreatorProfileReqVO;
import com.contenthub.web.model.vo.CreatorDashboardVO;
import com.contenthub.web.model.vo.CreatorProfileVO;
import com.contenthub.web.service.CreatorService;
import com.contenthub.web.util.CurrentUserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CreatorServiceImpl implements CreatorService {

    private final CreatorProfileMapper creatorProfileMapper;
    private final UserMapper userMapper;
    private final ContentMapper contentMapper;
    private final FavoriteMapper favoriteMapper;
    private final CommentMapper commentMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final SubscriptionPlanMapper planMapper;

    public CreatorServiceImpl(CreatorProfileMapper creatorProfileMapper,
                             UserMapper userMapper,
                             ContentMapper contentMapper,
                             FavoriteMapper favoriteMapper,
                             CommentMapper commentMapper,
                             SubscriptionMapper subscriptionMapper,
                             SubscriptionPlanMapper planMapper) {
        this.creatorProfileMapper = creatorProfileMapper;
        this.userMapper = userMapper;
        this.contentMapper = contentMapper;
        this.favoriteMapper = favoriteMapper;
        this.commentMapper = commentMapper;
        this.subscriptionMapper = subscriptionMapper;
        this.planMapper = planMapper;
    }

    @Override
    @Transactional
    public Response<CreatorProfileVO> apply() {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();

        if ("CREATOR".equals(loginUser.getRole()) || "ADMIN".equals(loginUser.getRole())) {
            // 已经是创作者，直接返回现有资料（幂等）
            return Response.success(toVO(loadOrCreate(loginUser)));
        }

        UserDO user = userMapper.selectById(loginUser.getUserId());
        if (Objects.isNull(user)) {
            throw new BizException(ResponseCodeEnum.UNAUTHORIZED);
        }

        userMapper.updateById(UserDO.builder()
                .id(user.getId())
                .role("CREATOR")
                .build());

        log.info("用户 {} 申请成为创作者，角色已更新为 CREATOR", user.getUsername());

        // 注意：本次请求内 SecurityContext 里缓存的还是旧角色，
        // 需要前端重新拉一次 /users/me 才能拿到新角色
        return Response.success(toVO(loadOrCreate(loginUser)));
    }

    @Override
    public Response<CreatorProfileVO> myProfile() {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();
        if (!"CREATOR".equals(loginUser.getRole()) && !"ADMIN".equals(loginUser.getRole())) {
            throw new BizException(ResponseCodeEnum.NOT_CREATOR);
        }
        return Response.success(toVO(loadOrCreate(loginUser)));
    }

    @Override
    @Transactional
    public Response<CreatorProfileVO> updateProfile(CreatorProfileReqVO req) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();
        if (!"CREATOR".equals(loginUser.getRole()) && !"ADMIN".equals(loginUser.getRole())) {
            throw new BizException(ResponseCodeEnum.NOT_CREATOR);
        }

        CreatorProfileDO profile = findProfile(loginUser.getUserId());
        if (profile == null) {
            profile = CreatorProfileDO.builder()
                    .userId(loginUser.getUserId())
                    .displayName(req.getDisplayName().trim())
                    .intro(req.getIntro())
                    .verified(false)
                    .subscriberCount(0)
                    .contentCount(0)
                    .build();
            creatorProfileMapper.insert(profile);
        } else {
            creatorProfileMapper.updateById(CreatorProfileDO.builder()
                    .id(profile.getId())
                    .displayName(req.getDisplayName().trim())
                    .intro(req.getIntro())
                    .build());
        }

        return Response.success(toVO(loadOrCreate(loginUser)));
    }

    @Override
    public Response<CreatorProfileVO> publicProfile(Long userId) {
        CreatorProfileDO profile = findProfile(userId);
        if (Objects.isNull(profile)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode(), "该创作者不存在");
        }
        return Response.success(toVO(profile));
    }

    @Override
    public Response<CreatorDashboardVO> dashboard() {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();
        Long creatorId = loginUser.getUserId();

        List<ContentDO> contents = contentMapper.selectList(new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getCreatorId, creatorId)
                .select(ContentDO::getId, ContentDO::getStatus,
                        ContentDO::getViewCount, ContentDO::getLikeCount));

        // 按状态计数
        Map<String, Long> byStatus = contents.stream()
                .collect(Collectors.groupingBy(ContentDO::getStatus, Collectors.counting()));

        long views = contents.stream().mapToLong(c -> c.getViewCount() == null ? 0 : c.getViewCount()).sum();
        long likes = contents.stream().mapToLong(c -> c.getLikeCount() == null ? 0 : c.getLikeCount()).sum();

        List<Long> contentIds = contents.stream().map(ContentDO::getId).toList();

        long favorites = 0;
        long comments = 0;
        if (!contentIds.isEmpty()) {
            Long favCount = favoriteMapper.selectCount(new LambdaQueryWrapper<FavoriteDO>()
                    .in(FavoriteDO::getContentId, contentIds));
            favorites = favCount == null ? 0 : favCount;

            Long commentCount = commentMapper.selectCount(new LambdaQueryWrapper<CommentDO>()
                    .in(CommentDO::getContentId, contentIds)
                    .eq(CommentDO::getStatus, "NORMAL"));
            comments = commentCount == null ? 0 : commentCount;
        }

        // 有效订阅人数：按用户去重，避免同一人多次续期被算成多个订阅者
        Long subscriberCount = subscriptionMapper.selectCount(new LambdaQueryWrapper<SubscriptionDO>()
                .eq(SubscriptionDO::getCreatorId, creatorId)
                .eq(SubscriptionDO::getStatus, "ACTIVE")
                .gt(SubscriptionDO::getEndTime, LocalDateTime.now()));

        Long planCount = planMapper.selectCount(new LambdaQueryWrapper<SubscriptionPlanDO>()
                .eq(SubscriptionPlanDO::getCreatorId, creatorId));

        return Response.success(CreatorDashboardVO.builder()
                .contentCount(contents.size())
                .publishedCount(byStatus.getOrDefault("PUBLISHED", 0L))
                .draftCount(byStatus.getOrDefault("DRAFT", 0L))
                .pendingCount(byStatus.getOrDefault("PENDING", 0L))
                .rejectedCount(byStatus.getOrDefault("REJECTED", 0L))
                .offlineCount(byStatus.getOrDefault("OFFLINE", 0L))
                .totalViews(views)
                .totalFavorites(favorites)
                // likeCount 是冗余字段，这里同时给出点赞与评论，避免前端只看到一种互动
                .totalComments(comments)
                .subscriberCount(subscriberCount == null ? 0 : subscriberCount)
                .planCount(planCount == null ? 0 : planCount)
                .build());
    }

    // ------------------------------------------------------------------ 内部方法

    /** 资料不存在时按当前用户名建一条，避免「有 CREATOR 角色却没有资料」的空状态 */
    private CreatorProfileDO loadOrCreate(LoginUser loginUser) {
        CreatorProfileDO profile = findProfile(loginUser.getUserId());
        if (profile != null) {
            return profile;
        }

        UserDO user = userMapper.selectById(loginUser.getUserId());
        String displayName = user != null && user.getNickname() != null
                ? user.getNickname()
                : loginUser.getUsername();

        profile = CreatorProfileDO.builder()
                .userId(loginUser.getUserId())
                .displayName(displayName)
                .intro(null)
                .verified(false)
                .subscriberCount(0)
                .contentCount(0)
                .build();
        creatorProfileMapper.insert(profile);
        return profile;
    }

    private CreatorProfileDO findProfile(Long userId) {
        return creatorProfileMapper.selectOne(new LambdaQueryWrapper<CreatorProfileDO>()
                .eq(CreatorProfileDO::getUserId, userId));
    }

    private CreatorProfileVO toVO(CreatorProfileDO profile) {
        Long published = contentMapper.selectCount(new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getCreatorId, profile.getUserId())
                .eq(ContentDO::getStatus, "PUBLISHED"));

        Long total = contentMapper.selectCount(new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getCreatorId, profile.getUserId()));

        return CreatorProfileVO.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .displayName(profile.getDisplayName())
                .intro(profile.getIntro())
                .verified(profile.getVerified())
                .subscriberCount(profile.getSubscriberCount())
                .contentCount(total == null ? 0 : total.intValue())
                .publishedCount(published == null ? 0L : published)
                .createTime(profile.getCreateTime())
                .build();
    }
}
