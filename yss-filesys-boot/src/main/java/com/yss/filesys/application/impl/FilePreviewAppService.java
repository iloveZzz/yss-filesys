package com.yss.filesys.application.impl;

import com.yss.filesys.application.dto.FilePreviewDTO;
import com.yss.filesys.application.port.FilePreviewUseCase;
import com.yss.filesys.domain.gateway.FileRecordGateway;
import com.yss.filesys.domain.model.BizException;
import com.yss.filesys.domain.model.FileRecord;
import com.yss.filesys.preview.PreviewTokenStore;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class FilePreviewAppService implements FilePreviewUseCase {

    private final FileRecordGateway fileRecordGateway;
    private final PreviewTokenStore previewTokenStore;

    public FilePreviewAppService(FileRecordGateway fileRecordGateway, PreviewTokenStore previewTokenStore) {
        this.fileRecordGateway = fileRecordGateway;
        this.previewTokenStore = previewTokenStore;
    }

    @Override
    public String issueToken(String fileId) {
        fileRecordGateway.findById(fileId).orElseThrow(() -> new BizException("文件不存在: " + fileId));
        return previewTokenStore.issue(fileId);
    }

    @Override
    public FilePreviewDTO preview(String fileId, String token) {
        if (token == null || token.isBlank()) {
            throw new BizException("预览 token 不能为空");
        }
        if (!previewTokenStore.verify(token, fileId)) {
            throw new BizException("预览 token 无效或已过期");
        }
        FileRecord fileRecord = fileRecordGateway.findById(fileId).orElseThrow(() -> new BizException("文件不存在: " + fileId));
        if (Boolean.TRUE.equals(fileRecord.getIsDir())) {
            throw new BizException("目录不支持预览");
        }
        String previewType = detectPreviewType(fileRecord);
        return FilePreviewDTO.builder()
                .fileId(fileRecord.getFileId())
                .fileName(fileRecord.getDisplayName())
                .mimeType(fileRecord.getMimeType())
                .previewType(previewType)
                .streamUrl("/files/stream/" + fileRecord.getFileId() + "?previewToken=" + token)
                .fileSize(fileRecord.getSize())
                .build();
    }

    @Override
    public String issueArchiveToken(String archiveFileId, String innerPath) {
        fileRecordGateway.findById(archiveFileId).orElseThrow(() -> new BizException("压缩包不存在: " + archiveFileId));
        if (innerPath == null || innerPath.isBlank()) {
            throw new BizException("innerPath 不能为空");
        }
        return previewTokenStore.issueForArchive(archiveFileId, innerPath);
    }

    @Override
    public FilePreviewDTO previewArchive(String archiveFileId, String innerPath, String token) {
        if (token == null || token.isBlank()) {
            throw new BizException("预览 token 不能为空");
        }
        if (innerPath == null || innerPath.isBlank()) {
            throw new BizException("innerPath 不能为空");
        }
        if (!previewTokenStore.verifyForArchive(token, archiveFileId, innerPath)) {
            throw new BizException("预览 token 无效或已过期");
        }
        FileRecord archive = fileRecordGateway.findById(archiveFileId).orElseThrow(() -> new BizException("压缩包不存在: " + archiveFileId));
        String fileName = innerPath.contains("/") ? innerPath.substring(innerPath.lastIndexOf('/') + 1) : innerPath;
        String suffix = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : "";
        String previewType = detectPreviewType(
                FileRecord.builder().suffix(suffix).mimeType(null).build()
        );
        String encodedInnerPath = URLEncoder.encode(innerPath, StandardCharsets.UTF_8);
        return FilePreviewDTO.builder()
                .fileId(archiveFileId)
                .fileName(fileName)
                .mimeType(null)
                .previewType(previewType)
                .streamUrl("/files/archive/stream/" + archive.getFileId() + "?innerPath=" + encodedInnerPath + "&previewToken=" + token)
                .fileSize(null)
                .build();
    }

    private String detectPreviewType(FileRecord fileRecord) {
        String suffix = fileRecord.getSuffix() == null ? "" : fileRecord.getSuffix().toLowerCase();
        String mime = fileRecord.getMimeType() == null ? "" : fileRecord.getMimeType().toLowerCase();
        if (mime.startsWith("image/")) {
            return "image";
        }
        if (mime.startsWith("video/")) {
            return "video";
        }
        if (mime.startsWith("audio/")) {
            return "audio";
        }
        if ("pdf".equals(suffix) || "application/pdf".equals(mime)) {
            return "document";
        }
        if (suffix.matches("md|markdown")) {
            return "markdown";
        }
        if (suffix.matches("java|js|ts|tsx|jsx|sql|py|go|cpp|c|h|hpp|cs|php|rb|rs|kt|swift|sh|bash")) {
            return "code";
        }
        if (suffix.matches("txt|json|xml|yaml|yml|csv|log|properties|conf|ini")) {
            return "text";
        }
        if (suffix.matches("doc|docx|xls|xlsx|ppt|pptx|odt|ods|odp")) {
            return "document";
        }
        return "unsupported";
    }
}
