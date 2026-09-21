package com.contenthub.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.contenthub.common.domain.dos.ContentDO;
import com.contenthub.common.domain.dos.CreatorProfileDO;
import com.contenthub.common.domain.dos.UserDO;
import com.contenthub.common.domain.mapper.ContentMapper;
import com.contenthub.common.domain.mapper.CreatorProfileMapper;
import com.contenthub.common.domain.mapper.UserMapper;
import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.common.utils.Response;
import com.contenthub.jwt.model.LoginUser;
import com.contenthub.web.model.req.CreatorProfileReqVO;
import com.contenthub.web.model.vo.CreatorProfileVO;
import com.contenthub.web.service.CreatorService;
import com.contenthub.web.util.CurrentUserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Slf4j
public class CreatorServiceImpl implements CreatorService {

    private final CreatorProfileMapper creatorProfileMapper;
    private final UserMapper userMapper;
    private final ContentMapper contentMapper;

    public CreatorServiceImpl(CreatorProfileMapper creatorProfileMapper,
                             UserMapper userMapper,
                             ContentMapper contentMapper) {
        this.creatorProfileMapper = creatorProfileMapper;
        this.userMapper = userMapper;
        this.contentMapper = contentMapper;
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
