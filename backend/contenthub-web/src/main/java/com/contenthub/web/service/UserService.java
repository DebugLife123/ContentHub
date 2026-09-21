package com.contenthub.web.service;

import com.contenthub.common.utils.Response;
import com.contenthub.web.model.vo.UserInfoVO;

public interface UserService {

    /**
     * 当前登录用户信息（计划表 19：GET /api/users/me）。
     */
    Response<UserInfoVO> currentUser();
}
