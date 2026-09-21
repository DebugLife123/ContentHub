package com.contenthub.web.service;

import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.CreatorProfileReqVO;
import com.contenthub.web.model.vo.CreatorDashboardVO;
import com.contenthub.web.model.vo.CreatorProfileVO;

public interface CreatorService {

    /**
     * 申请成为创作者（计划 Day 20：创作者身份）。
     *
     * <p>把 {@code users.role} 从 USER 升为 CREATOR 并建立 creator_profiles 记录，
     * 否则注册用户没有任何途径获得发布内容的权限。</p>
     */
    Response<CreatorProfileVO> apply();

    /** 我的创作者资料（不存在则按当前用户懒创建一条） */
    Response<CreatorProfileVO> myProfile();

    Response<CreatorProfileVO> updateProfile(CreatorProfileReqVO req);

    /** 公开的创作者资料 */
    Response<CreatorProfileVO> publicProfile(Long userId);

    /** 创作者仪表盘统计（阶段 6 Day 48） */
    Response<CreatorDashboardVO> dashboard();
}
