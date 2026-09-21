package com.contenthub.web.service;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.vo.ReadingHistoryVO;

public interface ReadingHistoryService {

    /**
     * 记录一次阅读（阶段 5 Day 46）。
     *
     * <p>未登录时静默跳过——内容详情是公开接口，游客浏览不需要留痕。</p>
     */
    void record(Long contentId, Integer progress);

    /** 更新阅读进度（前端滚动时回传） */
    Response<Void> updateProgress(Long contentId, int progress);

    /** 我的阅读历史 */
    Response<PageResponse<ReadingHistoryVO>> myHistory(long pageNum, long pageSize);

    /** 阅读过的内容数 */
    long readCount(Long userId);
}
