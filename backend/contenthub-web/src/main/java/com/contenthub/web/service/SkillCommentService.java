package com.contenthub.web.service;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.vo.SkillCommentVO;

/** Skill 评论 */
public interface SkillCommentService {

    /** 某个 Skill 下的评论（公开，只返回正常状态） */
    Response<PageResponse<SkillCommentVO>> listBySkill(Long skillId, long pageNum, long pageSize);

    /** 发表评论（需登录；Skill 必须已上架） */
    Response<SkillCommentVO> create(Long skillId, String body);

    /** 我的评论（跨 Skill，供个人中心用） */
    Response<PageResponse<SkillCommentVO>> myComments(long pageNum, long pageSize);

    /** 删除（本人或管理员） */
    Response<Void> delete(Long commentId);

    // ---------------------------------------------------------------- 管理端

    Response<PageResponse<SkillCommentVO>> pageForAdmin(long pageNum, long pageSize, String status);

    /** 隐藏 / 恢复 */
    Response<Void> updateStatus(Long commentId, String status);
}
