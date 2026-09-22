package com.contenthub.web.service;

import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.SkillCategoryReqVO;
import com.contenthub.web.model.vo.SkillCategoryVO;

import java.util.List;

/** Skill 分类（Skill 商城的栏目，与内容库分类相互独立） */
public interface SkillCategoryService {

    /**
     * @param enabledOnly true 只返回启用中的（公开接口用），false 返回全部（管理端用）
     */
    Response<List<SkillCategoryVO>> list(boolean enabledOnly);

    Response<SkillCategoryVO> create(SkillCategoryReqVO req);

    Response<SkillCategoryVO> update(Long id, SkillCategoryReqVO req);

    Response<Void> delete(Long id);
}
