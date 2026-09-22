package com.contenthub.web.service;

import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.CreatorProfileReqVO;
import com.contenthub.web.model.vo.CreatorDashboardVO;
import com.contenthub.web.model.vo.CreatorProfileVO;

public interface CreatorService {

    // 申请成为创作者已挪到 CreatorApplicationService：不再是「调用即提权」，
    // 而是「提交申请 -> 管理员审核 -> 通过才升级角色」。

    /** 我的创作者资料（不存在则按当前用户懒创建一条） */
    Response<CreatorProfileVO> myProfile();

    Response<CreatorProfileVO> updateProfile(CreatorProfileReqVO req);

    /** 公开的创作者资料 */
    Response<CreatorProfileVO> publicProfile(Long userId);

    /** 创作者仪表盘统计（阶段 6 Day 48） */
    Response<CreatorDashboardVO> dashboard();
}
