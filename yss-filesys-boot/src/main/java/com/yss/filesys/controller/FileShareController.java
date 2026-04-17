package com.yss.filesys.controller;

import com.yss.filesys.application.command.CreateShareCommand;
import com.yss.filesys.application.command.CreateShareAccessRecordCommand;
import com.yss.filesys.application.command.VerifyShareCodeCommand;
import com.yss.filesys.application.dto.FileDownloadDTO;
import com.yss.filesys.application.dto.FileShareDTO;
import com.yss.filesys.application.dto.FileShareAccessRecordDTO;
import com.yss.filesys.application.dto.FileRecordDTO;
import com.yss.filesys.application.dto.FileShareThinDTO;
import com.yss.filesys.application.port.FileShareCommandUseCase;
import com.yss.filesys.application.port.FileShareAccessUseCase;
import com.yss.filesys.application.port.FileShareQueryUseCase;
import com.yss.filesys.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/shares")
@Tag(name = "文件分享")
public class FileShareController {

    private final FileShareCommandUseCase fileShareCommandUseCase;
    private final FileShareQueryUseCase fileShareQueryUseCase;
    private final FileShareAccessUseCase fileShareAccessUseCase;

    public FileShareController(FileShareCommandUseCase fileShareCommandUseCase,
                               FileShareQueryUseCase fileShareQueryUseCase,
                               FileShareAccessUseCase fileShareAccessUseCase) {
        this.fileShareCommandUseCase = fileShareCommandUseCase;
        this.fileShareQueryUseCase = fileShareQueryUseCase;
        this.fileShareAccessUseCase = fileShareAccessUseCase;
    }

    @PostMapping
    @Operation(summary = "创建分享")
    public ApiResponse<FileShareDTO> create(@Valid @RequestBody CreateShareCommand command) {
        return ApiResponse.ok(fileShareCommandUseCase.create(command));
    }

    @GetMapping
    @Operation(summary = "按用户查询分享")
    public ApiResponse<List<FileShareDTO>> listByUser(@RequestParam String userId) {
        return ApiResponse.ok(fileShareQueryUseCase.listByUserId(userId));
    }

    @GetMapping("/{shareId}")
    @Operation(summary = "查询分享详情")
    public ApiResponse<FileShareDTO> getById(@PathVariable String shareId) {
        return ApiResponse.ok(fileShareQueryUseCase.getById(shareId));
    }

    @GetMapping("/{shareId}/info")
    @Operation(summary = "获取分享页信息")
    public ApiResponse<FileShareThinDTO> getShareInfo(@PathVariable String shareId) {
        return ApiResponse.ok(fileShareQueryUseCase.getShareInfo(shareId));
    }

    @PostMapping("/{shareId}/verify-code")
    @Operation(summary = "验证分享提取码")
    public ApiResponse<Boolean> verifyShareCode(@PathVariable String shareId, @Valid @RequestBody VerifyShareCodeCommand command) {
        command.setShareId(shareId);
        return ApiResponse.ok(fileShareQueryUseCase.verifyShareCode(command.getShareId(), command.getShareCode()));
    }

    @GetMapping("/{shareId}/items")
    @Operation(summary = "获取分享文件列表")
    public ApiResponse<List<FileRecordDTO>> listShareFiles(@PathVariable String shareId,
                                                           @RequestParam(required = false) String shareCode) {
        return ApiResponse.ok(fileShareQueryUseCase.listShareFiles(shareId, shareCode));
    }

    @GetMapping("/{shareId}/download/{fileId}")
    @Operation(summary = "下载分享内文件")
    public ResponseEntity<byte[]> downloadShareFile(@PathVariable String shareId,
                                                    @PathVariable String fileId,
                                                    @RequestParam(required = false) String shareCode) {
        FileDownloadDTO download = fileShareQueryUseCase.downloadShareFile(shareId, fileId, shareCode);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + URLEncoder.encode(download.getFileName(), StandardCharsets.UTF_8) + "\"")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(download.getContent().length))
                .body(download.getContent());
    }

    @DeleteMapping
    @Operation(summary = "批量取消分享")
    public ApiResponse<Void> cancel(@RequestBody List<String> shareIds) {
        fileShareCommandUseCase.cancelByIds(shareIds);
        return ApiResponse.ok();
    }

    @PostMapping("/{shareId}/access-records")
    @Operation(summary = "记录分享访问")
    public ApiResponse<Void> recordAccess(@PathVariable String shareId, @RequestBody CreateShareAccessRecordCommand command) {
        command.setShareId(shareId);
        fileShareAccessUseCase.record(command);
        return ApiResponse.ok();
    }

    @GetMapping("/{shareId}/access-records")
    @Operation(summary = "查询分享访问记录")
    public ApiResponse<List<FileShareAccessRecordDTO>> listAccessRecords(@PathVariable String shareId) {
        return ApiResponse.ok(fileShareAccessUseCase.listByShareId(shareId));
    }
}
