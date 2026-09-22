package com.contenthub.web.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储。
 *
 * <p>本地磁盘实现，够用且没有额外依赖。换对象存储时只要替换这个接口的实现，
 * Controller 与前端都不用动。</p>
 */
public interface FileStorageService {

    /** 保存文件，返回可直接放进正文的地址 */
    UploadResult store(MultipartFile file);

    /**
     * 按相对路径读取。
     *
     * @param relativePath 形如 {@code 2026/09/uuid.png}，由 store 生成，不接受用户自由输入
     */
    Resource load(String relativePath);

    /**
     * @param url 可直接写进 Markdown 的地址
     * @param name 原始文件名，给 {@code @file: 名称 | url} 用
     */
    record UploadResult(String url, String name, long size, String contentType) {
    }
}
