package com.contenthub.web.service.impl;

import com.contenthub.common.domain.dos.UserDO;
import com.contenthub.common.domain.mapper.UserMapper;
import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.common.utils.Response;
import com.contenthub.jwt.model.LoginUser;
import com.contenthub.web.model.req.ChangePasswordReqVO;
import com.contenthub.web.model.req.UserProfileReqVO;
import com.contenthub.web.model.vo.UserInfoVO;
import com.contenthub.web.service.UserService;
import com.contenthub.web.util.CurrentUserUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Response<UserInfoVO> currentUser() {
        return Response.success(toVO(requireCurrentUser()));
    }

    @Override
    @Transactional
    public Response<UserInfoVO> updateProfile(UserProfileReqVO req) {
        UserDO user = requireCurrentUser();

        // 用 updateById 传 null 会被 MyBatis-Plus 忽略，等于「清空昵称」改不动。
        // 编辑表单是整体提交的，这里把空串当成清空处理。
        userMapper.updateById(UserDO.builder()
                .id(user.getId())
                .nickname(normalize(req.getNickname()))
                .avatar(normalize(req.getAvatar()))
                .email(normalize(req.getEmail()))
                .bio(normalize(req.getBio()))
                .build());

        return Response.success(toVO(requireCurrentUser()));
    }

    @Override
    @Transactional
    public Response<Void> changePassword(ChangePasswordReqVO req) {
        UserDO user = requireCurrentUser();

        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BizException(ResponseCodeEnum.OLD_PASSWORD_WRONG);
        }

        userMapper.updateById(UserDO.builder()
                .id(user.getId())
                .password(passwordEncoder.encode(req.getNewPassword()))
                .build());

        // 刻意不在这里清 token：JWT 是无状态的，清 Redis 里的会话语义交给退出登录。
        // 真要「改密后强制下线」，应该在这里扫 login:token:* 并把该用户的全删掉。
        return Response.success();
    }

    // ------------------------------------------------------------------ 内部

    private UserDO requireCurrentUser() {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();
        // 从数据库读最新资料，而不是直接用 token 里的旧快照
        UserDO user = userMapper.selectById(loginUser.getUserId());
        if (Objects.isNull(user)) {
            throw new BizException(ResponseCodeEnum.UNAUTHORIZED);
        }
        return user;
    }

    /**
     * 空白串按「清空」处理。
     *
     * <p>MyBatis-Plus 的 {@code updateById} 会跳过 null 字段，所以清空必须写空串而不是 null——
     * 写 null 等于「这次不改这个字段」。传 null（客户端根本没带这个字段）则保持原值。</p>
     */
    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private UserInfoVO toVO(UserDO user) {
        return UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .bio(user.getBio())
                .role(user.getRole())
                .build();
    }
}
