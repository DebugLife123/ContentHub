package com.contenthub.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.contenthub.common.domain.dos.ContentDO;
import com.contenthub.common.domain.mapper.ContentMapper;
import com.contenthub.common.utils.Response;
import com.contenthub.web.service.ContentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContentServiceImpl implements ContentService {
    private final ContentMapper contentMapper;

    public ContentServiceImpl(ContentMapper contentMapper) {
        this.contentMapper = contentMapper;
    }

    @Override
    public Response<List<ContentDO>> listPublished(String contentType) {
        LambdaQueryWrapper<ContentDO> query = new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getStatus, "PUBLISHED")
                .eq(ContentDO::getDeleted, false)
                .orderByDesc(ContentDO::getCreateTime);
        if (contentType != null && !contentType.trim().isEmpty()) {
            query.eq(ContentDO::getContentType, contentType.trim().toUpperCase());
        }
        return Response.success(contentMapper.selectList(query));
    }

    @Override
    public Response<ContentDO> findPublishedById(Long id) {
        ContentDO content = contentMapper.selectOne(new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getId, id)
                .eq(ContentDO::getStatus, "PUBLISHED")
                .eq(ContentDO::getDeleted, false));
        if (content == null) {
            return Response.fail("CONTENT_NOT_FOUND", "内容不存在或尚未发布");
        }
        return Response.success(content);
    }
}
