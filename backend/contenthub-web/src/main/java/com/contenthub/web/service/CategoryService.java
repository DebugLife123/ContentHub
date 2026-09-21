package com.contenthub.web.service;

import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.CategoryReqVO;
import com.contenthub.web.model.vo.CategoryVO;

import java.util.List;

public interface CategoryService {

    /** 查询分类；enabledOnly=true 时只返回启用中的分类（公开接口用） */
    Response<List<CategoryVO>> list(boolean enabledOnly);

    /** 新增分类 */
    Response<CategoryVO> create(CategoryReqVO req);

    /** 修改分类 */
    Response<CategoryVO> update(Long id, CategoryReqVO req);

    /** 删除分类（逻辑删除） */
    Response<Void> delete(Long id);
}
