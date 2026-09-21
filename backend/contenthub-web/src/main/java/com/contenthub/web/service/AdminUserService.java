package com.contenthub.web.service;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.vo.AdminUserVO;

/** 管理端用户管理（阶段 6 Day 51） */
public interface AdminUserService {

    /** 分页查询用户，可按关键词与角色筛选 */
    Response<PageResponse<AdminUserVO>> page(String keyword, String role, long pageNum, long pageSize);

    /** 启用 / 禁用账号；禁用后该账号无法登录 */
    Response<Void> updateStatus(Long userId, String status);
}
