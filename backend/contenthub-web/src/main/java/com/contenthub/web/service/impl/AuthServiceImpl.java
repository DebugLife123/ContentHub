package com.contenthub.web.service.impl;

import com.contenthub.common.domain.dos.UserDO;
import com.contenthub.common.domain.mapper.UserMapper;
import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.common.utils.Response;
import com.contenthub.jwt.service.LoginTokenService;
import com.contenthub.web.model.req.RegisterReqVO;
import com.contenthub.web.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final LoginTokenService loginTokenService;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder,
                           LoginTokenService loginTokenService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.loginTokenService = loginTokenService;
    }

    @Override
    @Transactional
    public Response<Void> register(RegisterReqVO req) {
        if (!Objects.equals(req.getPassword(), req.getConfirmPassword())) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode(), "两次输入的密码不一致");
        }

        String username = req.getUsername().trim();
        if (Objects.nonNull(userMapper.findByUsername(username))) {
            throw new BizException(ResponseCodeEnum.USERNAME_EXISTS);
        }

        UserDO user = UserDO.builder()
                .username(username)
                // BCrypt 自带盐，数据库里不保存明文
                .password(passwordEncoder.encode(req.getPassword()))
                .nickname(username)
                // 计划 Day 6：注册只创建普通用户，创作者身份在阶段 3 单独申请
                .role("USER")
                .build();

        userMapper.insert(user);
        log.info("新用户注册成功：{}", username);

        return Response.success();
    }

    @Override
    public Response<Void> logout(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        if (StringUtils.isBlank(token)) {
            return Response.success();
        }

        boolean removed = loginTokenService.remove(token);
        log.info("用户退出登录，token 从 Redis 移除：{}", removed ? "成功" : "该 token 已不存在");
        return Response.success();
    }

    /** 从 "Bearer xxx" 中取出 xxx */
    private String extractToken(String authorizationHeader) {
        if (StringUtils.isBlank(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return StringUtils.substring(authorizationHeader, 7).trim();
    }
}
