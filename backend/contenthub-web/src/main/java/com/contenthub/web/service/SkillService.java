package com.contenthub.web.service;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.SkillPageReqVO;
import com.contenthub.web.model.req.SkillReqVO;
import com.contenthub.web.model.vo.SkillDetailVO;
import com.contenthub.web.model.vo.SkillListVO;

/**
 * Skill 商城（管理端维护，没有创作者投稿与审核环节）。
 *
 * <p>公开读接口只返回已上架（{@code PUBLISHED}）的 Skill；管理端接口能看到全部状态。</p>
 */
public interface SkillService {

    // ---------------------------------------------------------------- 公开读

    /** 已上架 Skill 分页（默认按星数倒序） */
    Response<PageResponse<SkillListVO>> pagePublished(SkillPageReqVO req);

    /**
     * 已上架 Skill 详情。
     *
     * <p>{@code accessType = MEMBER} 且当前用户不是会员时，安装命令与快速上手步骤
     * 不会下发，只返回 {@code locked = true} 与锁定原因。</p>
     */
    Response<SkillDetailVO> findPublishedById(Long id);

    // ---------------------------------------------------------------- 管理端

    /** 管理端分页（含草稿与已下架，可按状态筛选） */
    Response<PageResponse<SkillListVO>> pageForAdmin(SkillPageReqVO req);

    /** 管理端详情（不限状态，管理员不受会员限制） */
    Response<SkillDetailVO> findByIdForAdmin(Long id);

    /** 新增；一律落库为 DRAFT */
    Response<Long> create(SkillReqVO req);

    Response<Void> update(Long id, SkillReqVO req);

    /** 逻辑删除 */
    Response<Void> delete(Long id);

    /** 上架（DRAFT / OFFLINE -> PUBLISHED） */
    Response<Void> publish(Long id);

    /** 下架（PUBLISHED -> OFFLINE） */
    Response<Void> offline(Long id);
}
