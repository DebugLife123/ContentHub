package com.contenthub.web.service;

import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.ChangePasswordReqVO;
import com.contenthub.web.model.req.UserProfileReqVO;
import com.contenthub.web.model.vo.UserInfoVO;

public interface UserService {

    /**
     * 当前登录用户信息（计划表 19：GET /api/users/me）。
     */
    Response<UserInfoVO> currentUser();

    /** 修改个人资料（昵称 / 头像 / 邮箱 / 简介；用户名与角色不可改） */
    Response<UserInfoVO> updateProfile(UserProfileReqVO req);

    /** 修改密码（需校验原密码） */
    Response<Void> changePassword(ChangePasswordReqVO req);
}
