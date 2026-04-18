package com.yss.filesys.controller;

import com.yss.filesys.application.dto.FilePreviewDTO;
import com.yss.filesys.application.port.FilePreviewUseCase;
import com.yss.cloud.dto.response.SingleResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件预览控制器
 * <p>
 * 提供文件预览令牌签发和预览信息获取接口
 * </p>
 */
@RestController
@RequestMapping("/preview")
@Tag(name = "文件预览")
public class FilePreviewController {

    /**
     * 文件预览用例
     */
    private final FilePreviewUseCase filePreviewUseCase;

    public FilePreviewController(FilePreviewUseCase filePreviewUseCase) {
        this.filePreviewUseCase = filePreviewUseCase;
    }

    /**
     * 获取预览令牌
     *
     * @param fileId 文件ID
     * @return 预览令牌
     */
    @PostMapping("/token/{fileId}")
    @Operation(summary = "获取预览 token")
    public SingleResult<String> issueToken(@PathVariable String fileId) {
        return SingleResult.of(filePreviewUseCase.issueToken(fileId));
    }

    /**
     * 获取预览信息
     *
     * @param fileId       文件ID
     * @param previewToken 预览令牌
     * @return 预览信息
     */
    @GetMapping("/{fileId}")
    @Operation(summary = "获取预览信息")
    public SingleResult<FilePreviewDTO> preview(@PathVariable String fileId, @RequestParam String previewToken) {
        return SingleResult.of(filePreviewUseCase.preview(fileId, previewToken));
    }

    /**
     * 获取压缩包内文件预览令牌
     *
     * @param archiveFileId 压缩包文件ID
     * @param innerPath     压缩包内文件路径
     * @return 预览令牌
     */
    @PostMapping("/archive/token/{archiveFileId}")
    @Operation(summary = "获取压缩包内文件预览 token")
    public SingleResult<String> issueArchiveToken(@PathVariable String archiveFileId, @RequestParam String innerPath) {
        return SingleResult.of(filePreviewUseCase.issueArchiveToken(archiveFileId, innerPath));
    }

    /**
     * 获取压缩包内文件预览信息
     *
     * @param archiveFileId 压缩包文件ID
     * @param innerPath     压缩包内文件路径
     * @param previewToken  预览令牌
     * @return 预览信息
     */
    @GetMapping("/archive/{archiveFileId}")
    @Operation(summary = "获取压缩包内文件预览信息")
    public SingleResult<FilePreviewDTO> previewArchive(@PathVariable String archiveFileId,
                                                       @RequestParam String innerPath,
                                                       @RequestParam String previewToken) {
        return SingleResult.of(filePreviewUseCase.previewArchive(archiveFileId, innerPath, previewToken));
    }
}
