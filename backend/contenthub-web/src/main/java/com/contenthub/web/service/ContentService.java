package com.contenthub.web.service;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.ContentPageReqVO;
import com.contenthub.web.model.req.ContentReqVO;
import com.contenthub.web.model.vo.ContentDetailVO;
import com.contenthub.web.model.vo.ContentListVO;

import java.util.List;

public interface ContentService {

    /** 公开分页：仅已发布内容 */
    Response<PageResponse<ContentListVO>> pagePublished(ContentPageReqVO req);

    /** 热门内容（阶段 5 Day 41：读取 Redis ZSet 排名） */
    Response<List<ContentListVO>> hotContents(int limit);

    /** 创作者查看自己的内容（含草稿/待审核/驳回/下架） */
    Response<PageResponse<ContentListVO>> pageMine(ContentPageReqVO req);

    /** 管理端：按状态筛选全部内容，供审核页使用 */
    Response<PageResponse<ContentListVO>> pageForReview(ContentPageReqVO req);

    /** 公开详情：仅已发布内容，且按订阅权限决定是否给完整正文 */
    Response<ContentDetailVO> findPublishedById(Long id);

    /** 作者视角详情：不限状态，始终返回完整内容（编辑回显用） */
    Response<ContentDetailVO> findMineById(Long id);

    /**
     * 管理端审核详情：不限状态、不做订阅权限判断，始终返回完整正文。
     *
     * <p>审核列表用的是 {@code ContentListVO}，它刻意不含 {@code body}；
     * 管理员要判断「这篇能不能过」就必须能看到正文，所以单独开一个接口，
     * 而不是把正文塞回列表（那会让列表接口重新变重）。</p>
     */
    Response<ContentDetailVO> findForReview(Long id);

    /** 新增内容：一律以 DRAFT 落库，发布必须走审核 */
    Response<Long> create(ContentReqVO req);

    /** 编辑内容（仅作者本人或管理员） */
    Response<Void> update(Long id, ContentReqVO req);

    /** 删除内容（逻辑删除，仅作者本人或管理员） */
    Response<Void> delete(Long id);

    // ---------------------------------------------------------- 状态流转（计划 Day 21-23）
    // DRAFT / REJECTED / OFFLINE --提交审核--> PENDING --管理员通过--> PUBLISHED
    //                                              \--管理员驳回--> REJECTED
    // PUBLISHED --作者下架--> OFFLINE

    /** 提交审核 */
    Response<Void> submit(Long id);

    /** 下架 */
    Response<Void> offline(Long id);

    /** 管理员审核通过 */
    Response<Void> approve(Long id);

    /** 管理员审核驳回 */
    Response<Void> reject(Long id, String reason);
}
