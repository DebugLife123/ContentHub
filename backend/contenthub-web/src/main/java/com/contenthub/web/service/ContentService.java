package com.contenthub.web.service;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.ContentPageReqVO;
import com.contenthub.web.model.req.ContentReqVO;
import com.contenthub.web.model.vo.ContentDetailVO;
import com.contenthub.web.model.vo.ContentListVO;

public interface ContentService {

    /** 公开分页：仅已发布内容 */
    Response<PageResponse<ContentListVO>> pagePublished(ContentPageReqVO req);

    /** 创作者查看自己的内容（含草稿、下架），需登录 */
    Response<PageResponse<ContentListVO>> pageMine(ContentPageReqVO req);

    /** 公开详情：仅已发布内容 */
    Response<ContentDetailVO> findPublishedById(Long id);

    /** 作者视角详情：不限状态，供编辑草稿/下架内容时回显（仅作者本人或管理员） */
    Response<ContentDetailVO> findMineById(Long id);

    /** 新增内容，返回新内容ID */
    Response<Long> create(ContentReqVO req);

    /** 编辑内容（仅作者本人或管理员） */
    Response<Void> update(Long id, ContentReqVO req);

    /** 删除内容（逻辑删除，仅作者本人或管理员） */
    Response<Void> delete(Long id);
}
