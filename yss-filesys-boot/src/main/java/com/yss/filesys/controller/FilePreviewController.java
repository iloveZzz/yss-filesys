package com.yss.filesys.controller;

import com.yss.filesys.application.dto.FilePreviewDTO;
import com.yss.filesys.application.port.FilePreviewUseCase;
import com.yss.filesys.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/preview")
@Tag(name = "文件预览")
public class FilePreviewController {

    private final FilePreviewUseCase filePreviewUseCase;

    public FilePreviewController(FilePreviewUseCase filePreviewUseCase) {
        this.filePreviewUseCase = filePreviewUseCase;
    }

    @PostMapping("/token/{fileId}")
    @Operation(summary = "获取预览 token")
    public ApiResponse<String> issueToken(@PathVariable String fileId) {
        return ApiResponse.ok(filePreviewUseCase.issueToken(fileId));
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "获取预览信息")
    public ApiResponse<FilePreviewDTO> preview(@PathVariable String fileId, @RequestParam String previewToken) {
        return ApiResponse.ok(filePreviewUseCase.preview(fileId, previewToken));
    }

    @PostMapping("/archive/token/{archiveFileId}")
    @Operation(summary = "获取压缩包内文件预览 token")
    public ApiResponse<String> issueArchiveToken(@PathVariable String archiveFileId, @RequestParam String innerPath) {
        return ApiResponse.ok(filePreviewUseCase.issueArchiveToken(archiveFileId, innerPath));
    }

    @GetMapping("/archive/{archiveFileId}")
    @Operation(summary = "获取压缩包内文件预览信息")
    public ApiResponse<FilePreviewDTO> previewArchive(@PathVariable String archiveFileId,
                                                      @RequestParam String innerPath,
                                                      @RequestParam String previewToken) {
        return ApiResponse.ok(filePreviewUseCase.previewArchive(archiveFileId, innerPath, previewToken));
    }
}
