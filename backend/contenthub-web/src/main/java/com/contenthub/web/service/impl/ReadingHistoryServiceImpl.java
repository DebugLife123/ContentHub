package com.contenthub.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.contenthub.common.domain.dos.ContentDO;
import com.contenthub.common.domain.dos.ReadingHistoryDO;
import com.contenthub.common.domain.mapper.ContentMapper;
import com.contenthub.common.domain.mapper.ReadingHistoryMapper;
import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.jwt.model.LoginUser;
import com.contenthub.web.model.vo.ReadingHistoryVO;
import com.contenthub.web.service.ReadingHistoryService;
import com.contenthub.web.util.CurrentUserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReadingHistoryServiceImpl implements ReadingHistoryService {

    private final ReadingHistoryMapper historyMapper;
    private final ContentMapper contentMapper;

    public ReadingHistoryServiceImpl(ReadingHistoryMapper historyMapper, ContentMapper contentMapper) {
        this.historyMapper = historyMapper;
        this.contentMapper = contentMapper;
    }

    @Override
    @Transactional
    public void record(Long contentId, Integer progress) {
        LoginUser loginUser = CurrentUserUtil.getLoginUser();
        if (loginUser == null || contentId == null) {
            return;
        }
        upsert(loginUser.getUserId(), contentId, progress);
    }

    @Override
    @Transactional
    public Response<Void> updateProgress(Long contentId, int progress) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();
        upsert(loginUser.getUserId(), contentId, Math.max(0, Math.min(100, progress)));
        return Response.success();
    }

    @Override
    public Response<PageResponse<ReadingHistoryVO>> myHistory(long pageNum, long pageSize) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();

        Page<ReadingHistoryDO> page = historyMapper.selectPage(Page.of(pageNum, pageSize),
                new LambdaQueryWrapper<ReadingHistoryDO>()
                        .eq(ReadingHistoryDO::getUserId, loginUser.getUserId())
                        .orderByDesc(ReadingHistoryDO::getLastReadTime)
                        .orderByDesc(ReadingHistoryDO::getId));

        Map<Long, ContentDO> contents = contents(page.getRecords().stream()
                .map(ReadingHistoryDO::getContentId).collect(Collectors.toSet()));

        List<ReadingHistoryVO> list = page.getRecords().stream()
                .map(h -> {
                    ContentDO c = contents.get(h.getContentId());
                    return ReadingHistoryVO.builder()
                            .id(h.getId())
                            .contentId(h.getContentId())
                            // 内容可能已被删除，这里用占位文案而不是让整行消失
                            .contentTitle(c == null ? "（内容已删除）" : c.getTitle())
                            .contentType(c == null ? null : c.getContentType())
                            .accessType(c == null ? null : c.getAccessType())
                            .progress(h.getProgress())
                            .lastReadTime(h.getLastReadTime())
                            .build();
                })
                .toList();

        return Response.success(PageResponse.<ReadingHistoryVO>builder()
                .list(list)
                .total(page.getTotal())
                .pageNum(page.getCurrent())
                .pageSize(page.getSize())
                .pages(page.getPages())
                .build());
    }

    @Override
    public long readCount(Long userId) {
        Long count = historyMapper.selectCount(new LambdaQueryWrapper<ReadingHistoryDO>()
                .eq(ReadingHistoryDO::getUserId, userId));
        return count == null ? 0L : count;
    }

    // ------------------------------------------------------------------ 内部方法

    /**
     * 同一用户 + 同一内容只保留一行。
     *
     * <p>表上有 {@code uk_user_content}，所以必须「先查再决定插入还是更新」，
     * 不能直接 INSERT。进度取较大值：读者往回翻时进度不应倒退。</p>
     */
    private void upsert(Long userId, Long contentId, Integer progress) {
        ReadingHistoryDO existing = historyMapper.selectOne(new LambdaQueryWrapper<ReadingHistoryDO>()
                .eq(ReadingHistoryDO::getUserId, userId)
                .eq(ReadingHistoryDO::getContentId, contentId));

        LocalDateTime now = LocalDateTime.now();
        int incoming = progress == null ? 0 : Math.max(0, Math.min(100, progress));

        if (existing == null) {
            historyMapper.insert(ReadingHistoryDO.builder()
                    .userId(userId)
                    .contentId(contentId)
                    .progress(incoming)
                    .lastReadTime(now)
                    .build());
            return;
        }

        int merged = Math.max(existing.getProgress() == null ? 0 : existing.getProgress(), incoming);
        historyMapper.updateById(ReadingHistoryDO.builder()
                .id(existing.getId())
                .progress(merged)
                .lastReadTime(now)
                .build());
    }

    private Map<Long, ContentDO> contents(Set<Long> contentIds) {
        List<Long> ids = contentIds.stream().filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return contentMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(ContentDO::getId, c -> c, (a, b) -> a));
    }
}
