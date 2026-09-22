package com.contenthub.web.service.impl;

import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.web.config.UploadProperties;
import com.contenthub.web.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyy/MM");

    private final UploadProperties props;
    private final Path root;

    public FileStorageServiceImpl(UploadProperties props) throws IOException {
        this.props = props;
        this.root = Paths.get(props.getDir()).toAbsolutePath().normalize();
        Files.createDirectories(root);
        log.info("文件上传目录：{}", root);
    }

    @Override
    public UploadResult store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResponseCodeEnum.FILE_EMPTY);
        }
        if (file.getSize() > props.getMaxSizeBytes()) {
            throw new BizException(ResponseCodeEnum.FILE_TOO_LARGE.getErrorCode(),
                    "文件超过 " + (props.getMaxSizeBytes() / 1024 / 1024) + "MB 限制");
        }

        String original = StringUtils.defaultIfBlank(file.getOriginalFilename(), "file");
        String ext = extensionOf(original);
        if (ext.isEmpty() || !props.getAllowedExtensions().contains(ext)) {
            throw new BizException(ResponseCodeEnum.FILE_TYPE_NOT_ALLOWED.getErrorCode(),
                    "不支持的文件类型：" + (ext.isEmpty() ? "无扩展名" : "." + ext)
                            + "，允许的有 " + String.join(" / ", props.getAllowedExtensions()));
        }

        // 落盘名完全由服务端生成，不含任何用户输入，从根上避免路径穿越
        String relative = LocalDate.now().format(DATE_DIR) + "/" + UUID.randomUUID() + "." + ext;
        Path target = root.resolve(relative).normalize();

        // 双保险：就算上面的拼接逻辑以后被改坏，也不允许落到 root 之外
        if (!target.startsWith(root)) {
            throw new BizException(ResponseCodeEnum.FILE_UPLOAD_FAILED);
        }

        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            log.error("保存上传文件失败 {}", target, e);
            throw new BizException(ResponseCodeEnum.FILE_UPLOAD_FAILED);
        }

        String url = props.getUrlPrefix() + "/" + relative;
        log.info("上传成功 {} -> {} ({} bytes)", original, url, file.getSize());
        return new UploadResult(url, original, file.getSize(), file.getContentType());
    }

    @Override
    public Resource load(String relativePath) {
        if (StringUtils.isBlank(relativePath)) {
            throw new BizException(ResponseCodeEnum.RESOURCE_NOT_FOUND);
        }
        Path target = root.resolve(relativePath).normalize();
        // 拒绝 ../../ 这类越权读取
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            throw new BizException(ResponseCodeEnum.RESOURCE_NOT_FOUND);
        }
        return new FileSystemResource(target);
    }

    /** 取小写扩展名；没有扩展名时返回空串 */
    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
