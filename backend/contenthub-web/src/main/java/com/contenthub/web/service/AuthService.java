package com.contenthub.web.service;

import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.RegisterReqVO;

public interface AuthService {

    /** 注册普通用户（计划表 19：POST /api/auth/register） */
    Response<Void> register(RegisterReqVO req);

    /**
     * 退出登录：从 Redis 删除 token，使其立即失效。
     *
     * @param authorizationHeader 原始 Authorization 头，形如 "Bearer xxx"
     */
    Response<Void> logout(String authorizationHeader);
}
