package com.yss.filesys.controller;

import com.yss.cloud.dto.response.MultiResult;
import com.yss.cloud.dto.response.PageResult;
import com.yss.cloud.dto.response.SingleResult;
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
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
/**
 * 文件管理控制器
 * <p>
 * 提供文件的增删改查、目录管理、回收站管理、收藏功能等接口
 * </p>
 */
@RestController
@RequestMapping("/files")
@Tag(name = "文件管理")
@Slf4j
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
     * @param sortField 排序字段
     * @param sortOrder 排序方向
     * @param pageIndex 页索引
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
                                            @RequestParam(required = false) String sortField,
                                            @RequestParam(required = false) String sortOrder,
                                            @RequestParam(defaultValue = "0") long pageIndex,
                                            @RequestParam(defaultValue = "20") long pageSize) {
        log.debug("查询文件列表: userId={}, parentId={}, keyword={}, fileType={}, isDir={}, isRecents={}, deleted={}, favorite={}, sortField={}, sortOrder={}, pageIndex={}, pageSize={}",
                AnonymousUserContext.userId(), parentId, keyword, fileType, isDir, isRecents, deleted, favorite, sortField, sortOrder, pageIndex, pageSize);
        FileSearchQuery query = new FileSearchQuery();
        query.setUserId(AnonymousUserContext.userId());
        query.setParentId(parentId);
        query.setKeyword(keyword);
        query.setFileType(fileType);
        query.setIsDir(isDir);
        query.setIsRecents(isRecents);
        query.setDeleted(deleted);
        query.setFavorite(favorite);
        query.setSortField(sortField);
        query.setSortOrder(sortOrder);
        query.setPageIndex(pageIndex);
        query.setPageSize(pageSize);
        PageDTO<FileRecordDTO> result = fileQueryUseCase.search(query);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getPageSize(), result.getPageIndex());
    }

    /**
     * 查询文件详情
     *
     * @param fileId 文件ID
     * @return 文件详情
     */
    @GetMapping("/{fileId}")
    @Operation(summary = "查询文件详情")
    public SingleResult<FileRecordDTO> getFileById(@PathVariable String fileId) {
        log.debug("查询文件详情: userId={}, fileId={}", AnonymousUserContext.userId(), fileId);
        return SingleResult.of(fileQueryUseCase.getById(fileId));
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
        log.info("创建目录请求: userId={}, parentId={}, folderName={}, storageSettingId={}",
                command.getUserId(), command.getParentId(), command.getFolderName(), command.getStorageSettingId());
        return SingleResult.of(fileCommandUseCase.createDirectory(command));
    }

    /**
     * 查询目录列表
     * @param parentId 父目录ID
     * @return 目录列表
     */
    @GetMapping("/dirs")
    @Operation(summary = "查询目录列表")
    public MultiResult<DirectoryTreeDTO> listDirs(@RequestParam(required = false) String parentId) {
        log.debug("查询目录列表: userId={}, parentId={}", AnonymousUserContext.userId(), parentId);
        return MultiResult.of(fileQueryUseCase.listDirs(AnonymousUserContext.userId(), parentId));
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
        log.info("重命名文件请求: userId={}, fileId={}, fileName={}", command.getUserId(), fileId, command.getFileName());
        fileCommandUseCase.renameFile(fileId, command);
        return SingleResult.buildSuccess();
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
        log.info("移动文件请求: userId={}, fileId={}, targetParentId={}", command.getUserId(), command.getFileId(), command.getTargetParentId());
        fileCommandUseCase.moveFile(command);
        return SingleResult.buildSuccess();
    }

    /**
     * 获取目录层级路径
     * @param dirId  目录ID
     * @return 目录层级路径列表
     */
    @GetMapping("/directory/{dirId}/path")
    @Operation(summary = "获取目录层级")
    public MultiResult<FileRecordDTO> getDirectoryTreePath(@PathVariable String dirId) {
        log.debug("查询目录路径: userId={}, dirId={}", AnonymousUserContext.userId(), dirId);
        return MultiResult.of(fileQueryUseCase.getDirectoryTreePath(AnonymousUserContext.userId(), dirId));
    }

    /**
     * 获取文件URL
     * @return 文件访问URL
     */
    @GetMapping("/url/{fileId}")
    @Operation(summary = "获取文件URL")
    public SingleResult<String> getFileUrl(@PathVariable String fileId,
                                            @RequestParam(required = false) Integer expireSeconds) {
        log.info("获取文件URL请求: userId={}, fileId={}, expireSeconds={}", AnonymousUserContext.userId(), fileId, expireSeconds);
        return SingleResult.of(fileQueryUseCase.getFileUrl(fileId, AnonymousUserContext.userId(), expireSeconds));
    }

    /**
     * 下载文件
     * @return 文件内容
     * @download
     */
    @GetMapping("/download/{fileId}")
    @Operation(summary = "下载文件")
    public void downloadFile(@PathVariable String fileId, HttpServletResponse response) throws IOException {
        log.info("下载文件请求: userId={}, fileId={}", AnonymousUserContext.userId(), fileId);
        FileDownloadDTO download = fileQueryUseCase.downloadFile(fileId, AnonymousUserContext.userId());
        writeAttachmentHeaders(response, download.getFileName(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setContentLength(download.getContent().length);
        response.getOutputStream().write(download.getContent());
        response.flushBuffer();
    }

    /**
     * 根据目录和文件名称下载文件
     * @param parentId 目录ID
     * @param fileName 文件名称
     * @return 文件内容
     * @download
     */
    @GetMapping("/download/by-name")
    @Operation(summary = "根据目录和文件名称下载文件")
    public void downloadFileByParentAndName(@RequestParam(required = false) String parentId,
                                            @RequestParam String fileName,
                                            HttpServletResponse response) throws IOException {
        log.info("按目录和文件名下载请求: userId={}, parentId={}, fileName={}", AnonymousUserContext.userId(), parentId, fileName);
        FileDownloadDTO download = fileQueryUseCase.downloadFileByParentAndName(parentId, fileName, AnonymousUserContext.userId());
        writeAttachmentHeaders(response, download.getFileName(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setContentLength(download.getContent().length);
        response.getOutputStream().write(download.getContent());
        response.flushBuffer();
    }

    /**
     * 批量下载文件(打包为zip)
     * @param fileIds 文件ID列表
     * @return zip压缩文件内容
     * @download
     */
    @PostMapping("/download/batch")
    @Operation(summary = "批量下载文件")
    public void downloadFilesBatch(@RequestBody List<String> fileIds, HttpServletResponse response) throws IOException {
        log.info("批量下载请求: userId={}, fileIds={}", AnonymousUserContext.userId(), fileIds == null ? 0 : fileIds.size());
        List<FileDownloadDTO> downloads = fileQueryUseCase.downloadFiles(fileIds, AnonymousUserContext.userId());
        String zipFileName = buildZipFileName(downloads);
        writeAttachmentHeaders(response, zipFileName, "application/zip");
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            java.util.Map<String, Integer> fileNameCount = new java.util.HashMap<>();
            for (FileDownloadDTO download : downloads) {
                String entryName = uniqueZipEntryName(download.getFileName(), fileNameCount);
                zos.putNextEntry(new ZipEntry(entryName));
                zos.write(download.getContent());
                zos.closeEntry();
            }
            zos.finish();
            response.flushBuffer();
        } catch (Exception e) {
            throw new IOException("批量下载失败: " + e.getMessage(), e);
        }
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
        log.info("移入回收站请求: userId={}, fileIds={}", AnonymousUserContext.userId(), command.getFileIds() == null ? 0 : command.getFileIds().size());
        fileCommandUseCase.moveToRecycleBin(command);
        return SingleResult.buildSuccess();
    }

    private void writeAttachmentHeaders(HttpServletResponse response, String fileName, String contentType) {
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + "\"");
        response.setContentType(contentType);
    }

    private String buildZipFileName(List<FileDownloadDTO> downloads) {
        if (downloads == null || downloads.isEmpty()) {
            return "files.zip";
        }
        String firstName = downloads.get(0).getFileName();
        String nameWithoutExt = firstName;
        int lastDotIndex = firstName.lastIndexOf(".");
        if (lastDotIndex > 0) {
            nameWithoutExt = firstName.substring(0, lastDotIndex);
        }
        return nameWithoutExt + "等.zip";
    }

    private String uniqueZipEntryName(String fileName, java.util.Map<String, Integer> fileNameCount) {
        int count = fileNameCount.getOrDefault(fileName, 0);
        fileNameCount.put(fileName, count + 1);
        if (count <= 0) {
            return fileName;
        }
        String nameWithoutExt = fileName;
        String ext = "";
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex > 0) {
            nameWithoutExt = fileName.substring(0, lastDotIndex);
            ext = fileName.substring(lastDotIndex);
        }
        return nameWithoutExt + "_" + count + ext;
    }

    /**
     * 分页查询回收站文件
     * @param keyword  搜索关键词
     * @param pageIndex 页索引
     * @param pageSize 每页大小
     * @return 回收站文件列表
     */
    @GetMapping("/recycle")
    @Operation(summary = "分页查询回收站文件")
    public PageResult<FileRecordDTO> listRecycleFiles(@RequestParam(required = false) String keyword,
                                             @RequestParam(defaultValue = "0") long pageIndex,
                                             @RequestParam(defaultValue = "20") long pageSize) {
        FileSearchQuery query = new FileSearchQuery();
        query.setUserId(AnonymousUserContext.userId());
        query.setKeyword(keyword);
        query.setDeleted(true);
        query.setPageIndex(pageIndex);
        query.setPageSize(pageSize);
        PageDTO<FileRecordDTO> result = fileQueryUseCase.search(query);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getPageSize(), result.getPageIndex());
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
        log.info("恢复回收站请求: userId={}, fileIds={}", command.getUserId(), command.getFileIds() == null ? 0 : command.getFileIds().size());
        fileRecycleUseCase.restore(command);
        return SingleResult.buildSuccess();
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
        return SingleResult.buildSuccess();
    }

    /**
     * 清空回收站
     *
     * @return 操作结果
     */
    @DeleteMapping("/recycle/clear")
    @Operation(summary = "清空回收站")
    public SingleResult<Void> clearRecycle() {
        ClearRecycleCommand command = new ClearRecycleCommand();
        command.setUserId(AnonymousUserContext.userId());
        fileRecycleUseCase.clearRecycle(command);
        return SingleResult.buildSuccess();
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
        log.info("收藏文件请求: userId={}, fileIds={}", command.getUserId(), command.getFileIds() == null ? 0 : command.getFileIds().size());
        fileFavoriteUseCase.favorite(command);
        return SingleResult.buildSuccess();
    }

    /**
     * 取消收藏文件
     *
     * @param command 取消收藏命令
     * @return 操作结果
     */
    @PostMapping("/unfavorite")
    @Operation(summary = "取消收藏文件")
    public SingleResult<Void> unfavorite(@Valid @RequestBody FavoriteFilesCommand command) {
        command.setUserId(AnonymousUserContext.userId());
        log.info("取消收藏请求: userId={}, fileIds={}", command.getUserId(), command.getFileIds() == null ? 0 : command.getFileIds().size());
        fileFavoriteUseCase.unfavorite(command);
        return SingleResult.buildSuccess();
    }

    /**
     * 获取收藏数量
     * @return 收藏数量
     */
    @GetMapping("/favorites/count")
    @Operation(summary = "获取收藏数量")
    public SingleResult<Long> favoritesCount() {
        return SingleResult.of(fileFavoriteUseCase.count(AnonymousUserContext.userId()));
    }
}
