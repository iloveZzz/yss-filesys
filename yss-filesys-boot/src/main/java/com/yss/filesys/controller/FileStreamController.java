package com.yss.filesys.controller;

import com.yss.filesys.domain.gateway.FileRecordGateway;
import com.yss.filesys.domain.model.BizException;
import com.yss.filesys.domain.model.FileRecord;
import com.yss.filesys.preview.PreviewTokenStore;
import com.yss.filesys.storage.plugin.boot.StorageServiceFacade;
import com.yss.filesys.storage.plugin.core.IStorageOperationService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 文件流控制器
 * <p>
 * 提供文件流式传输接口，支持断点续传和压缩包内文件流传输
 * </p>
 */
@RestController
@RequestMapping("/api/files")
public class FileStreamController {

    /**
     * 文件记录网关
     */
    private final FileRecordGateway fileRecordGateway;
    /**
     * 预览令牌存储
     */
    private final PreviewTokenStore previewTokenStore;
    /**
     * 存储服务门面
     */
    private final StorageServiceFacade storageServiceFacade;

    public FileStreamController(FileRecordGateway fileRecordGateway,
                                PreviewTokenStore previewTokenStore,
                                StorageServiceFacade storageServiceFacade) {
        this.fileRecordGateway = fileRecordGateway;
        this.previewTokenStore = previewTokenStore;
        this.storageServiceFacade = storageServiceFacade;
    }

    /**
     * 文件流式传输
     * <p>
     * 支持断点续传，通过 Range 请求头实现
     * </p>
     *
     * @param fileId       文件ID
     * @param previewToken 预览令牌
     * @param rangeHeader  Range请求头
     * @return 文件流响应
     */
    @GetMapping("/stream/{fileId}")
    public ResponseEntity<?> stream(@PathVariable String fileId,
                                    @RequestParam String previewToken,
                                    @RequestHeader(value = "Range", required = false) String rangeHeader) {
        if (!previewTokenStore.verify(previewToken, fileId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid preview token");
        }
        FileRecord fileRecord = fileRecordGateway.findById(fileId).orElseThrow(() -> new BizException("文件不存在: " + fileId));
        IStorageOperationService storageService = storageServiceFacade.getStorageService(fileRecord.getStorageSettingId());
        if (!storageService.isFileExist(fileRecord.getObjectKey())) {
            throw new BizException("文件不存在或尚未落盘");
        }
        try {
            String contentType = fileRecord.getMimeType() == null || fileRecord.getMimeType().isBlank()
                    ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                    : fileRecord.getMimeType();
            long fileLength = fileRecord.getSize() == null ? 0L : fileRecord.getSize();
            if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileLength))
                        .body(new InputStreamResource(storageService.downloadFile(fileRecord.getObjectKey())));
            }
            long[] range = parseRange(rangeHeader, fileLength);
            long start = range[0];
            long end = range[1];
            int contentLength = (int) (end - start + 1);
            try (InputStream in = storageService.downloadFileRange(fileRecord.getObjectKey(), start, end)) {
                byte[] data = in.readAllBytes();
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength)
                        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                        .body(data);
            }
        } catch (IOException e) {
            throw new BizException("读取文件失败: " + e.getMessage());
        }
    }

    /**
     * 压缩包内文件流式传输
     *
     * @param archiveFileId 压缩包文件ID
     * @param innerPath     压缩包内文件路径
     * @param previewToken  预览令牌
     * @return 文件流响应
     */
    @GetMapping("/archive/stream/{archiveFileId}")
    public ResponseEntity<?> streamArchiveInner(@PathVariable String archiveFileId,
                                                @RequestParam String innerPath,
                                                @RequestParam String previewToken) {
        String decodedInnerPath = URLDecoder.decode(innerPath, StandardCharsets.UTF_8);
        if (!previewTokenStore.verifyForArchive(previewToken, archiveFileId, decodedInnerPath)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid preview token");
        }
        FileRecord archive = fileRecordGateway.findById(archiveFileId).orElseThrow(() -> new BizException("压缩包不存在: " + archiveFileId));
        IStorageOperationService storageService = storageServiceFacade.getStorageService(archive.getStorageSettingId());
        if (!storageService.isFileExist(archive.getObjectKey())) {
            throw new BizException("压缩包文件不存在");
        }
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("yss-archive-", ".zip");
            try (InputStream in = storageService.downloadFile(archive.getObjectKey())) {
                Files.copy(in, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            try (ZipFile zipFile = new ZipFile(tempFile.toFile())) {
                ZipEntry entry = zipFile.getEntry(decodedInnerPath);
                if (entry == null || entry.isDirectory()) {
                    throw new BizException("压缩包内文件不存在: " + decodedInnerPath);
                }
                byte[] data;
                try (InputStream in = zipFile.getInputStream(entry)) {
                    data = in.readAllBytes();
                }
                String contentType = contentTypeByName(decodedInnerPath);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(data.length))
                        .body(data);
            }
        } catch (IOException e) {
            throw new BizException("读取压缩包内文件失败: " + e.getMessage());
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * 解析 Range 请求头
     *
     * @param rangeHeader Range请求头
     * @param fileLength  文件总长度
     * @return 起始和结束位置数组
     */
    private long[] parseRange(String rangeHeader, long fileLength) {
        String range = rangeHeader.substring("bytes=".length());
        String[] parts = range.split("-", 2);
        long start = parts[0].isBlank() ? 0 : Long.parseLong(parts[0]);
        long end = parts.length > 1 && !parts[1].isBlank() ? Long.parseLong(parts[1]) : fileLength - 1;
        if (start < 0 || end >= fileLength || start > end) {
            throw new BizException("非法 Range 请求");
        }
        return new long[]{start, end};
    }

    /**
     * 根据文件名获取内容类型
     *
     * @param name 文件名
     * @return MIME类型
     */
    private String contentTypeByName(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".mp4")) {
            return "video/mp4";
        }
        if (lower.endsWith(".mp3")) {
            return "audio/mpeg";
        }
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".json") || lower.endsWith(".xml")
                || lower.endsWith(".yaml") || lower.endsWith(".yml") || lower.endsWith(".csv")) {
            return "text/plain;charset=UTF-8";
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
