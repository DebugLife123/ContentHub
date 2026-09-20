package com.contenthub.web.service;

import com.contenthub.common.domain.dos.ContentDO;
import com.contenthub.common.utils.Response;

import java.util.List;

public interface ContentService {
    Response<List<ContentDO>> listPublished(String contentType);

    Response<ContentDO> findPublishedById(Long id);
}
