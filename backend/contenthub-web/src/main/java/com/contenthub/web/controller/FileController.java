package com.contenthub.web.controller;

import com.contenthub.common.utils.Response;
import com.contenthub.web.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文件上传与读取。
 *
 * <p>读取刻意走 Controller 而不是 WebMvc 的静态资源映射：路径校验逻辑
 * （拒绝 {@code ../} 越权、只允许 root 之下）放在自己手里更清楚，
 * 也方便按扩展名给 Content-Type 与缓存头。</p>
 */
@RestController
@Tag(name = "文件")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/files/upload")
    @Operation(summary = "上传文件（登录即可；白名单扩展名 + 大小限制）")
    public Response<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        FileStorageService.UploadResult result = fileStorageService.store(file);

        // 用 LinkedHashMap 而不是 Map.of：键顺序稳定，前端调试时看得舒服
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("url", result.url());
        data.put("name", result.name());
        data.put("size", result.size());
        data.put("contentType", result.contentType());
        // 直接给可粘贴的两种写法，省得用户自己拼
        data.put("markdownImage", "![" + result.name() + "](" + result.url() + ")");
        data.put("markdownFile", "@file: " + result.name() + " | " + result.url());
        return Response.success(data);
    }

    @GetMapping("/files/**")
    @Operation(summary = "读取已上传的文件（公开，内容正文里的图片/附件要用）")
    public ResponseEntity<Resource> read(HttpServletRequest request) {
        // 用 requestURI 而不是 @PathVariable：后者拿不到 /files/ 之后的多级路径
        String uri = request.getRequestURI();
        String prefix = request.getContextPath() + "/files/";
        String relative = uri.startsWith(prefix) ? uri.substring(prefix.length()) : "";
        int q = relative.indexOf('?');
        if (q >= 0) {
            relative = relative.substring(0, q);
        }

        Resource resource = fileStorageService.load(relative);

        return ResponseEntity.ok()
                .contentType(mediaTypeOf(relative))
                // 文件名由服务端生成且内容不变，可以放心长缓存
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(resource);
    }

    /** 按扩展名给 Content-Type；认不出来就当作二进制流 */
    private MediaType mediaTypeOf(String relative) {
        int dot = relative.lastIndexOf('.');
        String ext = dot < 0 ? "" : relative.substring(dot + 1).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.parseMediaType("image/webp");
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "mp4" -> MediaType.parseMediaType("video/mp4");
            case "webm" -> MediaType.parseMediaType("video/webm");
            case "mp3" -> MediaType.parseMediaType("audio/mpeg");
            case "txt", "md" -> MediaType.TEXT_PLAIN;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
