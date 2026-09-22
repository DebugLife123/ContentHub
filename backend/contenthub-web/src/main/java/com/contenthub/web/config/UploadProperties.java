package com.contenthub.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文件上传配置。
 *
 * <p>对应 application*.yml 里的 {@code contenthub.upload.*}。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "contenthub.upload")
public class UploadProperties {

    /** 落盘目录。dev 用项目下的 ./uploads，容器里用挂载卷 /app/uploads */
    private String dir = "./uploads";

    /**
     * 对外的 URL 前缀。要带上 server.servlet.context-path，
     * 否则浏览器直接请求 /files/xxx 会 404（前端 axios 的 baseURL 是 /api，
     * 但 Markdown 里的图片地址是浏览器直接发的，不走 axios）。
     */
    private String urlPrefix = "/api/files";

    /** 单文件大小上限（字节），同时受 spring.servlet.multipart.max-file-size 限制 */
    private long maxSizeBytes = 50L * 1024 * 1024;

    /**
     * 允许的扩展名（小写，不含点）。
     *
     * <p>刻意不含 svg：SVG 可以内嵌 script，同源直出等于给自己开一个 XSS 口子。
     * 也不接受任意扩展名——白名单比黑名单可靠。</p>
     */
    private List<String> allowedExtensions = List.of(
            // 图片
            "jpg", "jpeg", "png", "gif", "webp",
            // 文档
            "pdf", "epub", "mobi", "txt", "md",
            // 压缩包（代码模板 / 数据集）
            "zip", "gz", "tar",
            // 音视频
            "mp4", "webm", "mp3", "m4a"
    );
}
