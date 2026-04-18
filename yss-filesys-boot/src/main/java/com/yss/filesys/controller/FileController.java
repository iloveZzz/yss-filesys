package com.yss.filesys.controller;

import com.yss.filesys.application.command.CreateDirectoryCommand;
import com.yss.filesys.application.command.ClearRecycleCommand;
import com.yss.filesys.application.command.FavoriteFilesCommand;
import com.yss.filesys.application.command.MoveFileCommand;
import com.yss.filesys.application.command.MoveToRecycleBinCommand;
import com.yss.filesys.application.command.PermanentlyDeleteRecycleCommand;
import com.yss.filesys.application.command.RenameFileCommand;
import com.yss.filesys.application.command.RestoreRecycleCommand;
import com.yss.filesys.application.dto.DirectoryTreeDTO;
import com.yss.filesys.application.dto.FileDownloadDTO;
import com.yss.filesys.application.dto.FileRecordDTO;
import com.yss.filesys.application.dto.PageDTO;
import com.yss.filesys.application.port.FileCommandUseCase;
import com.yss.filesys.application.port.FileFavoriteUseCase;
import com.yss.filesys.application.port.FileQueryUseCase;
import com.yss.filesys.application.port.FileRecycleUseCase;
import com.yss.filesys.application.query.FileSearchQuery;
import com.yss.filesys.common.AnonymousUserContext;
import com.yss.filesys.common.MultiResult;
import com.yss.filesys.common.PageResult;
import com.yss.filesys.common.SingleResult;
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
/**
 * 文件管理控制器
 * <p>
 * 提供文件的增删改查、目录管理、回收站管理、收藏功能等接口
 * </p>
 */
@RestController
@RequestMapping("/api/files")
@Tag(name = "文件管理")
public class FileController {

    /**
     * 文件查询用例
     */
    private final FileQueryUseCase fileQueryUseCase;
    /**
     * 文件命令用例
     */
    private final FileCommandUseCase fileCommandUseCase;
    /**
     * 回收站用例
     */
    private final FileRecycleUseCase fileRecycleUseCase;
    /**
     * 文件收藏用例
     */
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

    /**
     * 分页查询文件
     * @param parentId 父目录ID
     * @param keyword  搜索关键词
     * @param deleted  是否已删除
     * @param favorite 是否收藏
     * @param pageNo   页码
     * @param pageSize 每页大小
     * @return 分页文件列表
     */
    @GetMapping
    @Operation(summary = "分页查询文件")
    public PageResult<FileRecordDTO> search(@RequestParam(required = false) String parentId,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String fileType,
                                            @RequestParam(required = false) Boolean isDir,
                                            @RequestParam(required = false) Boolean isRecents,
                                            @RequestParam(required = false) Boolean deleted,
                                            @RequestParam(required = false) Boolean favorite,
                                            @RequestParam(defaultValue = "1") long pageNo,
                                            @RequestParam(defaultValue = "20") long pageSize) {
        FileSearchQuery query = new FileSearchQuery();
        query.setUserId(AnonymousUserContext.userId());
        query.setParentId(parentId);
        query.setKeyword(keyword);
        query.setFileType(fileType);
        query.setIsDir(isDir);
        query.setIsRecents(isRecents);
        query.setDeleted(deleted);
        query.setFavorite(favorite);
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        return PageResult.ok(fileQueryUseCase.search(query));
    }

    /**
     * 查询文件详情
     *
     * @param fileId 文件ID
     * @return 文件详情
     */
    @GetMapping("/{fileId}")
    @Operation(summary = "查询文件详情")
    public SingleResult<FileRecordDTO> getById(@PathVariable String fileId) {
        return SingleResult.ok(fileQueryUseCase.getById(fileId));
    }

    /**
     * 创建目录
     *
     * @param command 创建目录命令
     * @return 创建的目录信息
     */
    @PostMapping("/directory")
    @Operation(summary = "创建目录")
    public SingleResult<FileRecordDTO> createDirectory(@Valid @RequestBody CreateDirectoryCommand command) {
        command.setUserId(AnonymousUserContext.userId());
        return SingleResult.ok(fileCommandUseCase.createDirectory(command));
    }

    /**
     * 查询目录列表
     * @param parentId 父目录ID
     * @return 目录列表
     */
    @GetMapping("/dirs")
    @Operation(summary = "查询目录列表")
    public MultiResult<DirectoryTreeDTO> listDirs(@RequestParam(required = false) String parentId) {
        return MultiResult.ok(fileQueryUseCase.listDirs(AnonymousUserContext.userId(), parentId));
    }

    /**
     * 文件重命名
     *
     * @param fileId  文件ID
     * @param command 重命名命令
     * @return 操作结果
     */
    @PutMapping("/{fileId}/rename")
    @Operation(summary = "文件重命名")
    public SingleResult<Void> renameFile(@PathVariable String fileId, @Valid @RequestBody RenameFileCommand command) {
        command.setUserId(AnonymousUserContext.userId());
        fileCommandUseCase.renameFile(fileId, command);
        return SingleResult.ok();
    }

    /**
     * 文件移动
     *
     * @param command 移动命令
     * @return 操作结果
     */
    @PutMapping("/moves")
    @Operation(summary = "文件移动")
    public SingleResult<Void> moveFile(@Valid @RequestBody MoveFileCommand command) {
        command.setUserId(AnonymousUserContext.userId());
        fileCommandUseCase.moveFile(command);
        return SingleResult.ok();
    }

    /**
     * 获取目录层级路径
     * @param dirId  目录ID
     * @return 目录层级路径列表
     */
    @GetMapping("/directory/{dirId}/path")
    @Operation(summary = "获取目录层级")
    public MultiResult<FileRecordDTO> getDirectoryTreePath(@PathVariable String dirId) {
        return MultiResult.ok(fileQueryUseCase.getDirectoryTreePath(AnonymousUserContext.userId(), dirId));
    }

    /**
     * 获取文件URL
     * @return 文件访问URL
     */
    @GetMapping("/url/{fileId}")
    @Operation(summary = "获取文件URL")
    public SingleResult<String> getFileUrl(@PathVariable String fileId,
                                            @RequestParam(required = false) Integer expireSeconds) {
        return SingleResult.ok(fileQueryUseCase.getFileUrl(fileId, AnonymousUserContext.userId(), expireSeconds));
    }

    /**
     * 下载文件
     * @return 文件内容
     */
    @GetMapping("/download/{fileId}")
    @Operation(summary = "下载文件")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileId) {
        FileDownloadDTO download = fileQueryUseCase.downloadFile(fileId, AnonymousUserContext.userId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + URLEncoder.encode(download.getFileName(), StandardCharsets.UTF_8) + "\"")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(download.getContent().length))
                .body(download.getContent());
    }

    /**
     * 批量移入回收站
     *
     * @param command 移入回收站命令
     * @return 操作结果
     */
    @DeleteMapping("/recycle")
    @Operation(summary = "批量移入回收站")
    public SingleResult<Void> moveToRecycle(@Valid @RequestBody MoveToRecycleBinCommand command) {
        fileCommandUseCase.moveToRecycleBin(command);
        return SingleResult.ok();
    }

    /**
     * 分页查询回收站文件
     * @param keyword  搜索关键词
     * @param pageNo   页码
     * @param pageSize 每页大小
     * @return 回收站文件列表
     */
    @GetMapping("/recycle")
    @Operation(summary = "分页查询回收站文件")
    public PageResult<FileRecordDTO> recycle(@RequestParam(required = false) String keyword,
                                             @RequestParam(defaultValue = "1") long pageNo,
                                             @RequestParam(defaultValue = "20") long pageSize) {
        FileSearchQuery query = new FileSearchQuery();
        query.setUserId(AnonymousUserContext.userId());
        query.setKeyword(keyword);
        query.setDeleted(true);
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        return PageResult.ok(fileQueryUseCase.search(query));
    }

    /**
     * 恢复回收站文件
     *
     * @param command 恢复命令
     * @return 操作结果
     */
    @PutMapping("/recycle/restore")
    @Operation(summary = "恢复回收站文件")
    public SingleResult<Void> restore(@Valid @RequestBody RestoreRecycleCommand command) {
        command.setUserId(AnonymousUserContext.userId());
        fileRecycleUseCase.restore(command);
        return SingleResult.ok();
    }

    /**
     * 永久删除回收站文件
     *
     * @param command 永久删除命令
     * @return 操作结果
     */
    @DeleteMapping("/recycle/permanent")
    @Operation(summary = "永久删除回收站文件")
    public SingleResult<Void> permanentlyDelete(@Valid @RequestBody PermanentlyDeleteRecycleCommand command) {
        command.setUserId(AnonymousUserContext.userId());
        fileRecycleUseCase.permanentlyDelete(command);
        return SingleResult.ok();
    }

    /**
     * 清空回收站
     *
     * @param command 清空回收站命令
     * @return 操作结果
     */
    @DeleteMapping("/recycle/clear")
    @Operation(summary = "清空回收站")
    public SingleResult<Void> clearRecycle(@Valid @RequestBody ClearRecycleCommand command) {
        command.setUserId(AnonymousUserContext.userId());
        fileRecycleUseCase.clearRecycle(command);
        return SingleResult.ok();
    }

    /**
     * 收藏文件
     *
     * @param command 收藏命令
     * @return 操作结果
     */
    @PostMapping("/favorites")
    @Operation(summary = "收藏文件")
    public SingleResult<Void> favorite(@Valid @RequestBody FavoriteFilesCommand command) {
        command.setUserId(AnonymousUserContext.userId());
        fileFavoriteUseCase.favorite(command);
        return SingleResult.ok();
    }

    /**
     * 取消收藏文件
     *
     * @param command 取消收藏命令
     * @return 操作结果
     */
    @DeleteMapping("/favorites")
    @Operation(summary = "取消收藏文件")
    public SingleResult<Void> unfavorite(@Valid @RequestBody FavoriteFilesCommand command) {
        command.setUserId(AnonymousUserContext.userId());
        fileFavoriteUseCase.unfavorite(command);
        return SingleResult.ok();
    }

    /**
     * 获取收藏数量
     * @return 收藏数量
     */
    @GetMapping("/favorites/count")
    @Operation(summary = "获取收藏数量")
    public SingleResult<Long> favoritesCount() {
        return SingleResult.ok(fileFavoriteUseCase.count(AnonymousUserContext.userId()));
    }
}
