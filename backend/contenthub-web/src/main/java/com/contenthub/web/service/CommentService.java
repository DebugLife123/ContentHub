package com.contenthub.web.service;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.CommentReqVO;
import com.contenthub.web.model.vo.CommentVO;

public interface CommentService {

    /** 某内容下的评论（只返回 NORMAL 状态） */
    Response<PageResponse<CommentVO>> listByContent(Long contentId, long pageNum, long pageSize);

    /** 发表评论（阶段 5 Day 45） */
    Response<CommentVO> create(Long contentId, CommentReqVO req);

    /** 删除自己的评论；管理员可删任意评论 */
    Response<Void> delete(Long commentId);

    /** 我的评论（个人中心用） */
    Response<PageResponse<CommentVO>> myComments(long pageNum, long pageSize);

    /** 某内容的评论数 */
    long countByContent(Long contentId);

    // ---------------------------------------------------------- 管理端（阶段 6 Day 54）

    /** 管理端评论列表，可按状态筛选（含已隐藏） */
    Response<PageResponse<CommentVO>> adminList(String status, long pageNum, long pageSize);

    /** 隐藏 / 恢复评论 */
    Response<Void> setStatus(Long commentId, String status);
}
