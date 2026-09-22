package com.contenthub.web.service;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.CreatorApplyReqVO;
import com.contenthub.web.model.vo.CreatorApplicationVO;

/**
 * 创作者申请。
 *
 * <p>改造前 {@code POST /creator/apply} 是直接把角色改成 CREATOR，
 * 任何注册用户一键就能发布内容。现在走「提交申请 → 管理员审核 → 通过才升级」，
 * 与内容审核同一种形态。</p>
 */
public interface CreatorApplicationService {

    /** 提交申请（已是创作者、或已有待审核申请时会被拒绝） */
    Response<CreatorApplicationVO> apply(CreatorApplyReqVO req);

    /** 我的最近一条申请，没有则返回 null（前端据此显示不同状态） */
    Response<CreatorApplicationVO> myApplication();

    // ---------------------------------------------------------------- 管理端

    Response<PageResponse<CreatorApplicationVO>> pageForReview(long pageNum, long pageSize, String status);

    /** 通过：申请置为 APPROVED，同时把用户角色升为 CREATOR */
    Response<Void> approve(Long id);

    /** 驳回：申请置为 REJECTED 并记录原因 */
    Response<Void> reject(Long id, String reason);

    /** 待审核条数，给管理端下拉菜单做角标 */
    Response<Long> pendingCount();
}
