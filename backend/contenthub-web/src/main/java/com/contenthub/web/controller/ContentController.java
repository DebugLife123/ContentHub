package com.contenthub.web.controller;

import com.contenthub.common.domain.dos.ContentDO;
import com.contenthub.common.utils.Response;
import com.contenthub.web.service.ContentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/contents")
public class ContentController {
    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    public Response<List<ContentDO>> list(@RequestParam(required = false) String contentType) {
        return contentService.listPublished(contentType);
    }

    @GetMapping("/{id}")
    public Response<ContentDO> detail(@PathVariable Long id) {
        return contentService.findPublishedById(id);
    }
}
