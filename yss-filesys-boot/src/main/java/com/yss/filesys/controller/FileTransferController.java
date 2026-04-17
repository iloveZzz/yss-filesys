package com.yss.filesys.controller;

import com.yss.filesys.application.command.CheckUploadCommand;
import com.yss.filesys.application.command.InitDownloadCommand;
import com.yss.filesys.application.command.InitTransferUploadCommand;
import com.yss.filesys.application.command.MergeChunksCommand;
import com.yss.filesys.application.command.UploadChunkCommand;
import com.yss.filesys.application.dto.CheckUploadResultDTO;
import com.yss.filesys.application.dto.FileTransferTaskDTO;
import com.yss.filesys.application.dto.InitDownloadResultDTO;
import com.yss.filesys.application.port.FileTransferCommandUseCase;
import com.yss.filesys.application.port.FileTransferQueryUseCase;
import com.yss.filesys.application.query.DownloadChunkQuery;
import com.yss.filesys.common.ApiResponse;
import com.yss.filesys.domain.model.FileRecord;
import com.yss.filesys.service.TransferSseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
@Tag(name = "文件传输")
public class FileTransferController {

    private final FileTransferCommandUseCase fileTransferCommandUseCase;
    private final FileTransferQueryUseCase fileTransferQueryUseCase;
    private final TransferSseService transferSseService;

    public FileTransferController(FileTransferCommandUseCase fileTransferCommandUseCase,
                                  FileTransferQueryUseCase fileTransferQueryUseCase,
                                  TransferSseService transferSseService) {
        this.fileTransferCommandUseCase = fileTransferCommandUseCase;
        this.fileTransferQueryUseCase = fileTransferQueryUseCase;
        this.transferSseService = transferSseService;
    }

    @GetMapping("/subscribe")
    @Operation(summary = "订阅传输进度")
    public SseEmitter subscribe(@RequestParam String userId) {
        return transferSseService.subscribe(userId);
    }

    @PostMapping("/upload/init")
    @Operation(summary = "初始化上传任务")
    public ApiResponse<FileTransferTaskDTO> initUpload(@Valid @RequestBody InitTransferUploadCommand command) {
        return ApiResponse.ok(fileTransferCommandUseCase.initUpload(command));
    }

    @PostMapping("/upload/check")
    @Operation(summary = "上传前 MD5 校验")
    public ApiResponse<CheckUploadResultDTO> checkUpload(@Valid @RequestBody CheckUploadCommand command) {
        return ApiResponse.ok(fileTransferCommandUseCase.checkUpload(command));
    }

    @PostMapping("/upload/chunk")
    @Operation(summary = "上传分片")
    public ApiResponse<Void> uploadChunk(@RequestParam("file") MultipartFile file,
                                         @RequestParam("taskId") String taskId,
                                         @RequestParam("chunkIndex") Integer chunkIndex,
                                         @RequestParam(value = "chunkMd5", required = false) String chunkMd5) throws Exception {
        UploadChunkCommand command = new UploadChunkCommand();
        command.setTaskId(taskId);
        command.setChunkIndex(chunkIndex);
        command.setChunkMd5(chunkMd5);
        fileTransferCommandUseCase.uploadChunk(command, file.getBytes());
        return ApiResponse.ok();
    }

    @PostMapping("/upload/merge")
    @Operation(summary = "手动触发合并")
    public ApiResponse<FileRecord> merge(@Valid @RequestBody MergeChunksCommand command) {
        return ApiResponse.ok(fileTransferCommandUseCase.mergeChunks(command));
    }

    @PostMapping("/download/init")
    @Operation(summary = "初始化下载任务")
    public ApiResponse<InitDownloadResultDTO> initDownload(@Valid @RequestBody InitDownloadCommand command) {
        return ApiResponse.ok(fileTransferCommandUseCase.initDownload(command));
    }

    @GetMapping("/download/chunk")
    @Operation(summary = "下载分片")
    public ResponseEntity<byte[]> downloadChunk(@Valid DownloadChunkQuery query) {
        byte[] bytes = fileTransferQueryUseCase.downloadChunk(query);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length))
                .body(bytes);
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "取消传输任务")
    public ApiResponse<Void> cancel(@PathVariable String taskId) {
        fileTransferCommandUseCase.cancel(taskId);
        return ApiResponse.ok();
    }

    @GetMapping
    @Operation(summary = "按用户查询传输任务")
    public ApiResponse<List<FileTransferTaskDTO>> listByUser(@RequestParam String userId) {
        return ApiResponse.ok(fileTransferQueryUseCase.listByUserId(userId));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "查询传输任务详情")
    public ApiResponse<FileTransferTaskDTO> getByTaskId(@PathVariable String taskId) {
        return ApiResponse.ok(fileTransferQueryUseCase.getByTaskId(taskId));
    }
}
