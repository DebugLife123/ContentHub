package com.contenthub.web.service.impl;

import com.contenthub.common.domain.dos.UserDO;
import com.contenthub.common.domain.mapper.UserMapper;
import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.common.utils.Response;
import com.contenthub.jwt.model.LoginUser;
import com.contenthub.web.model.vo.UserInfoVO;
import com.contenthub.web.service.UserService;
import com.contenthub.web.util.CurrentUserUtil;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public Response<UserInfoVO> currentUser() {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();

        // 从数据库读最新资料，而不是直接用 token 里的旧快照
        UserDO user = userMapper.selectById(loginUser.getUserId());
        if (Objects.isNull(user)) {
            throw new BizException(ResponseCodeEnum.UNAUTHORIZED);
        }

        return Response.success(UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .bio(user.getBio())
                .role(user.getRole())
                .build());
    }
}
