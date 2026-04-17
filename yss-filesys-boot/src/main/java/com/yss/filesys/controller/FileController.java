package com.yss.filesys.controller;

import com.yss.filesys.application.command.CreateDirectoryCommand;
import com.yss.filesys.application.command.ClearRecycleCommand;
import com.yss.filesys.application.command.FavoriteFilesCommand;
import com.yss.filesys.application.command.MoveFileCommand;
import com.yss.filesys.application.command.MoveToRecycleBinCommand;
import com.yss.filesys.application.command.PermanentlyDeleteRecycleCommand;
import com.yss.filesys.application.command.RenameFileCommand;
import com.yss.filesys.application.command.RestoreRecycleCommand;
import com.yss.filesys.application.dto.FileDownloadDTO;
import com.yss.filesys.application.dto.FileRecordDTO;
import com.yss.filesys.application.dto.PageDTO;
import com.yss.filesys.application.port.FileCommandUseCase;
import com.yss.filesys.application.port.FileFavoriteUseCase;
import com.yss.filesys.application.port.FileQueryUseCase;
import com.yss.filesys.application.port.FileRecycleUseCase;
import com.yss.filesys.application.query.FileSearchQuery;
import com.yss.filesys.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@Tag(name = "文件管理")
public class FileController {

    private final FileQueryUseCase fileQueryUseCase;
    private final FileCommandUseCase fileCommandUseCase;
    private final FileRecycleUseCase fileRecycleUseCase;
    private final FileFavoriteUseCase fileFavoriteUseCase;

    public FileController(FileQueryUseCase fileQueryUseCase,
                          FileCommandUseCase fileCommandUseCase,
                          FileRecycleUseCase fileRecycleUseCase,
                          FileFavoriteUseCase fileFavoriteUseCase) {
        this.fileQueryUseCase = fileQueryUseCase;
        this.fileCommandUseCase = fileCommandUseCase;
        this.fileRecycleUseCase = fileRecycleUseCase;
        this.fileFavoriteUseCase = fileFavoriteUseCase;
    }

    @GetMapping
    @Operation(summary = "分页查询文件")
    public ApiResponse<PageDTO<FileRecordDTO>> search(@RequestParam String userId,
                                                      @RequestParam(required = false) String parentId,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) Boolean deleted,
                                                      @RequestParam(required = false) Boolean favorite,
                                                      @RequestParam(defaultValue = "1") long pageNo,
                                                      @RequestParam(defaultValue = "20") long pageSize) {
        FileSearchQuery query = new FileSearchQuery();
        query.setUserId(userId);
        query.setParentId(parentId);
        query.setKeyword(keyword);
        query.setDeleted(deleted);
        query.setFavorite(favorite);
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        return ApiResponse.ok(fileQueryUseCase.search(query));
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "查询文件详情")
    public ApiResponse<FileRecordDTO> getById(@PathVariable String fileId) {
        return ApiResponse.ok(fileQueryUseCase.getById(fileId));
    }

    @PostMapping("/directory")
    @Operation(summary = "创建目录")
    public ApiResponse<FileRecordDTO> createDirectory(@Valid @RequestBody CreateDirectoryCommand command) {
        return ApiResponse.ok(fileCommandUseCase.createDirectory(command));
    }

    @GetMapping("/dirs")
    @Operation(summary = "查询目录列表")
    public ApiResponse<java.util.List<FileRecordDTO>> listDirs(@RequestParam String userId,
                                                               @RequestParam(required = false) String parentId) {
        return ApiResponse.ok(fileQueryUseCase.listDirs(userId, parentId));
    }

    @PutMapping("/{fileId}/rename")
    @Operation(summary = "文件重命名")
    public ApiResponse<Void> renameFile(@PathVariable String fileId, @Valid @RequestBody RenameFileCommand command) {
        fileCommandUseCase.renameFile(fileId, command);
        return ApiResponse.ok();
    }

    @PutMapping("/moves")
    @Operation(summary = "文件移动")
    public ApiResponse<Void> moveFile(@Valid @RequestBody MoveFileCommand command) {
        fileCommandUseCase.moveFile(command);
        return ApiResponse.ok();
    }

    @GetMapping("/directory/{dirId}/path")
    @Operation(summary = "获取目录层级")
    public ApiResponse<java.util.List<FileRecordDTO>> getDirectoryTreePath(@RequestParam String userId,
                                                                           @PathVariable String dirId) {
        return ApiResponse.ok(fileQueryUseCase.getDirectoryTreePath(userId, dirId));
    }

    @GetMapping("/url/{fileId}")
    @Operation(summary = "获取文件URL")
    public ApiResponse<String> getFileUrl(@PathVariable String fileId,
                                          @RequestParam String userId,
                                          @RequestParam(required = false) Integer expireSeconds) {
        return ApiResponse.ok(fileQueryUseCase.getFileUrl(fileId, userId, expireSeconds));
    }

    @GetMapping("/download/{fileId}")
    @Operation(summary = "下载文件")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileId,
                                               @RequestParam String userId) {
        FileDownloadDTO download = fileQueryUseCase.downloadFile(fileId, userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + URLEncoder.encode(download.getFileName(), StandardCharsets.UTF_8) + "\"")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(download.getContent().length))
                .body(download.getContent());
    }

    @DeleteMapping("/recycle")
    @Operation(summary = "批量移入回收站")
    public ApiResponse<Void> moveToRecycle(@Valid @RequestBody MoveToRecycleBinCommand command) {
        fileCommandUseCase.moveToRecycleBin(command);
        return ApiResponse.ok();
    }

    @GetMapping("/recycle")
    @Operation(summary = "分页查询回收站文件")
    public ApiResponse<PageDTO<FileRecordDTO>> recycle(@RequestParam String userId,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(defaultValue = "1") long pageNo,
                                                      @RequestParam(defaultValue = "20") long pageSize) {
        FileSearchQuery query = new FileSearchQuery();
        query.setUserId(userId);
        query.setKeyword(keyword);
        query.setDeleted(true);
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        return ApiResponse.ok(fileQueryUseCase.search(query));
    }

    @PutMapping("/recycle/restore")
    @Operation(summary = "恢复回收站文件")
    public ApiResponse<Void> restore(@Valid @RequestBody RestoreRecycleCommand command) {
        fileRecycleUseCase.restore(command);
        return ApiResponse.ok();
    }

    @DeleteMapping("/recycle/permanent")
    @Operation(summary = "永久删除回收站文件")
    public ApiResponse<Void> permanentlyDelete(@Valid @RequestBody PermanentlyDeleteRecycleCommand command) {
        fileRecycleUseCase.permanentlyDelete(command);
        return ApiResponse.ok();
    }

    @DeleteMapping("/recycle/clear")
    @Operation(summary = "清空回收站")
    public ApiResponse<Void> clearRecycle(@Valid @RequestBody ClearRecycleCommand command) {
        fileRecycleUseCase.clearRecycle(command);
        return ApiResponse.ok();
    }

    @PostMapping("/favorites")
    @Operation(summary = "收藏文件")
    public ApiResponse<Void> favorite(@Valid @RequestBody FavoriteFilesCommand command) {
        fileFavoriteUseCase.favorite(command);
        return ApiResponse.ok();
    }

    @DeleteMapping("/favorites")
    @Operation(summary = "取消收藏文件")
    public ApiResponse<Void> unfavorite(@Valid @RequestBody FavoriteFilesCommand command) {
        fileFavoriteUseCase.unfavorite(command);
        return ApiResponse.ok();
    }

    @GetMapping("/favorites/count")
    @Operation(summary = "获取收藏数量")
    public ApiResponse<Long> favoritesCount(@RequestParam String userId) {
        return ApiResponse.ok(fileFavoriteUseCase.count(userId));
    }
}
