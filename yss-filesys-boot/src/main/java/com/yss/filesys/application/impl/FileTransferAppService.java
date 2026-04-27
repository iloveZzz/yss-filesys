package com.yss.filesys.application.impl;

import com.yss.filesys.application.command.CheckUploadCommand;
import com.yss.filesys.application.command.InitDownloadCommand;
import com.yss.filesys.application.command.InitTransferUploadCommand;
import com.yss.filesys.application.command.MergeChunksCommand;
import com.yss.filesys.application.command.UploadChunkCommand;
import com.yss.filesys.application.dto.CheckUploadResultDTO;
import com.yss.filesys.application.dto.FileTransferTaskDTO;
import com.yss.filesys.application.dto.FileTransferStatsDTO;
import com.yss.filesys.application.dto.PageDTO;
import com.yss.filesys.application.dto.InitDownloadResultDTO;
import com.yss.filesys.application.port.FileTransferCommandUseCase;
import com.yss.filesys.application.port.FileTransferQueryUseCase;
import com.yss.filesys.application.query.DownloadChunkQuery;
import com.yss.filesys.common.AnonymousUserContext;
import com.yss.filesys.domain.gateway.FileRecordGateway;
import com.yss.filesys.domain.gateway.FileShareItemGateway;
import com.yss.filesys.domain.gateway.FileUserFavoriteGateway;
import com.yss.filesys.domain.gateway.FileTransferTaskGateway;
import com.yss.filesys.domain.model.BizException;
import com.yss.filesys.domain.model.FileRecord;
import com.yss.filesys.domain.model.FileTransferTask;
import com.yss.filesys.domain.model.TransferTaskStatus;
import com.yss.filesys.domain.model.TransferTaskType;
import com.yss.filesys.application.service.UploadDestinationReservationService;
import com.yss.filesys.storage.plugin.boot.StorageServiceFacade;
import com.yss.filesys.storage.plugin.core.IStorageOperationService;
import com.yss.filesys.storage.plugin.core.config.StorageUtils;
import com.yss.filesys.service.TransferSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileTransferAppService implements FileTransferCommandUseCase, FileTransferQueryUseCase {

    private final FileTransferTaskGateway fileTransferTaskGateway;
    private final FileRecordGateway fileRecordGateway;
    private final FileShareItemGateway fileShareItemGateway;
    private final FileUserFavoriteGateway fileUserFavoriteGateway;
    private final StorageServiceFacade storageServiceFacade;
    private final TransferSseService transferSseService;
    private final UploadDestinationReservationService uploadDestinationReservationService;

    @Value("${yss.files.storage-root:/tmp/yss-filesys/storage}")
    private String storageRoot;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileTransferTaskDTO initUpload(InitTransferUploadCommand command) {
        command.setUserId(resolveUserId(command.getUserId()));
        String taskId = UUID.randomUUID().toString().replace("-", "");
        uploadDestinationReservationService.reserve(taskId, command.getUserId(), command.getParentId(), command.getFileName());
        try {
            log.info("收到上传初始化请求: userId={}, parentId={}, fileName={}, fileSize={}, totalChunks={}, chunkSize={}, overwriteExisting={}",
                    command.getUserId(),
                    command.getParentId(),
                    command.getFileName(),
                    command.getFileSize(),
                    command.getTotalChunks(),
                    command.getChunkSize(),
                    resolveOverwriteExisting(command.getOverwriteExisting()));
            LocalDateTime now = LocalDateTime.now();
            String storageSettingId = resolveStorageSettingId(command.getStorageSettingId());
            String objectKey = buildObjectKey(command.getUserId(), taskId, command.getFileName());
            FileTransferTask task = FileTransferTask.builder()
                    .taskId(taskId)
                    .uploadId(taskId)
                    .parentId(command.getParentId())
                    .userId(command.getUserId())
                    .storageSettingId(storageSettingId)
                    .objectKey(objectKey)
                    .fileName(command.getFileName())
                    .fileSize(command.getFileSize())
                    .mimeType(command.getMimeType())
                    .suffix(suffix(command.getFileName()))
                    .totalChunks(command.getTotalChunks())
                    .chunkSize(command.getChunkSize())
                    .taskType(TransferTaskType.upload)
                    .uploadedChunks(0)
                    .uploadedSize(0L)
                    .overwriteExisting(resolveOverwriteExisting(command.getOverwriteExisting()))
                    .status(TransferTaskStatus.initialized)
                    .startTime(now)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            FileTransferTask saved = fileTransferTaskGateway.save(task);
            log.info("上传任务初始化完成: taskId={}, userId={}, storageSettingId={}, objectKey={}",
                    saved.getTaskId(), saved.getUserId(), saved.getStorageSettingId(), saved.getObjectKey());
            transferSseService.sendStatus(saved.getUserId(), saved.getTaskId(), saved.getStatus().name(), "任务已初始化");
            return toDTO(saved);
        } catch (RuntimeException e) {
            uploadDestinationReservationService.release(taskId);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckUploadResultDTO checkUpload(CheckUploadCommand command) {
        FileTransferTask task = fileTransferTaskGateway.findByTaskId(command.getTaskId())
                .orElseThrow(() -> new BizException("传输任务不存在: " + command.getTaskId()));
        if (task.getStatus() != TransferTaskStatus.initialized) {
            throw new BizException("任务状态不支持校验: " + task.getStatus());
        }
        log.info("开始上传校验: taskId={}, userId={}, fileMd5={}", task.getTaskId(), task.getUserId(), command.getFileMd5());
        uploadDestinationReservationService.touch(task.getTaskId());
        transferSseService.sendStatus(task.getUserId(), task.getTaskId(), TransferTaskStatus.checking.name(), "开始校验");

        var existed = fileRecordGateway.findByUserAndMd5(task.getUserId(), command.getFileMd5());
        if (existed.isPresent()) {
            FileRecord source = existed.get();
            IStorageOperationService storageService = storageServiceFacade.getStorageService(source.getStorageSettingId());
            if (storageService.isFileExist(source.getObjectKey())) {
                LocalDateTime now = LocalDateTime.now();
                prepareUploadDestination(task);
                FileRecord duplicated = FileRecord.builder()
                        .fileId(UUID.randomUUID().toString().replace("-", ""))
                        .objectKey(source.getObjectKey())
                        .originalName(task.getFileName())
                        .displayName(task.getFileName())
                        .suffix(task.getSuffix())
                        .size(task.getFileSize())
                        .mimeType(task.getMimeType())
                        .isDir(false)
                        .parentId(task.getParentId())
                        .userId(task.getUserId())
                        .contentMd5(command.getFileMd5())
                        .storageSettingId(task.getStorageSettingId())
                        .uploadTime(now)
                        .updateTime(now)
                        .isDeleted(false)
                        .build();
                fileRecordGateway.save(duplicated);

                fileTransferTaskGateway.save(task.toBuilder()
                        .fileMd5(command.getFileMd5())
                        .fileId(duplicated.getFileId())
                        .uploadedChunks(task.getTotalChunks())
                        .uploadedSize(task.getFileSize())
                        .status(TransferTaskStatus.completed)
                        .completeTime(now)
                        .updatedAt(now)
                        .overwriteExisting(task.getOverwriteExisting())
                        .build());
                log.info("命中秒传: taskId={}, userId={}, fileId={}, overwriteExisting={}",
                        task.getTaskId(), task.getUserId(), duplicated.getFileId(), task.getOverwriteExisting());
                transferSseService.sendComplete(task.getUserId(), task.getTaskId(), "命中秒传");
                uploadDestinationReservationService.release(task.getTaskId());
                return CheckUploadResultDTO.builder()
                        .instantUpload(true)
                        .taskId(task.getTaskId())
                        .status(TransferTaskStatus.completed.name())
                        .message("命中秒传")
                        .build();
            }
        }

        FileTransferTask checked = task.toBuilder()
                .fileMd5(command.getFileMd5())
                .status(TransferTaskStatus.uploading)
                .updatedAt(LocalDateTime.now())
                .build();
        fileTransferTaskGateway.save(checked);
        log.info("秒传未命中，进入分片上传: taskId={}, userId={}, fileName={}, totalChunks={}",
                task.getTaskId(), task.getUserId(), task.getFileName(), task.getTotalChunks());
        transferSseService.sendStatus(task.getUserId(), task.getTaskId(), TransferTaskStatus.uploading.name(), "进入上传中");
        return CheckUploadResultDTO.builder()
                .instantUpload(false)
                .taskId(task.getTaskId())
                .status(TransferTaskStatus.uploading.name())
                .message("未命中秒传，进入上传中状态")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadChunk(UploadChunkCommand command, byte[] bytes) {
        FileTransferTask task = fileTransferTaskGateway.findByTaskId(command.getTaskId())
                .orElseThrow(() -> new BizException("传输任务不存在: " + command.getTaskId()));
        if (task.getStatus() != TransferTaskStatus.uploading) {
            throw new BizException("任务状态不支持上传分片: " + task.getStatus());
        }
        log.debug("接收上传分片: taskId={}, userId={}, chunkIndex={}, chunkSize={}",
                task.getTaskId(), task.getUserId(), command.getChunkIndex(), bytes == null ? 0 : bytes.length);
        uploadDestinationReservationService.touch(task.getTaskId());
        Path chunkDir = chunkDir(task.getTaskId());
        try {
            Files.createDirectories(chunkDir);
            Path chunkFile = chunkDir.resolve(command.getChunkIndex() + ".part");
            if (!Files.exists(chunkFile)) {
                Files.write(chunkFile, bytes);
            }
            int uploadedChunks = countUploadedChunks(chunkDir);
            long uploadedSize = calcUploadedSize(chunkDir);
            FileTransferTask updated = task.toBuilder()
                    .uploadedChunks(uploadedChunks)
                    .uploadedSize(uploadedSize)
                    .updatedAt(LocalDateTime.now())
                    .build();
            fileTransferTaskGateway.save(updated);
            transferSseService.sendProgress(task.getUserId(), task.getTaskId(), uploadedSize, task.getFileSize(), uploadedChunks, task.getTotalChunks());
            if (uploadedChunks >= task.getTotalChunks()) {
                MergeChunksCommand merge = new MergeChunksCommand();
                merge.setTaskId(task.getTaskId());
                mergeChunks(merge);
            }
        } catch (IOException e) {
            log.error("上传分片失败: taskId={}, userId={}, chunkIndex={}, message={}",
                    task.getTaskId(), task.getUserId(), command.getChunkIndex(), e.getMessage(), e);
            fileTransferTaskGateway.updateStatus(task.getTaskId(), TransferTaskStatus.failed, e.getMessage());
            uploadDestinationReservationService.release(task.getTaskId());
            throw new BizException("上传分片失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileRecord mergeChunks(MergeChunksCommand command) {
        FileTransferTask task = fileTransferTaskGateway.findByTaskId(command.getTaskId())
                .orElseThrow(() -> new BizException("传输任务不存在: " + command.getTaskId()));
        uploadDestinationReservationService.touch(task.getTaskId());
        if (task.getStatus() == TransferTaskStatus.completed) {
            if (task.getFileId() == null || task.getFileId().isBlank()) {
                throw new BizException("任务已完成，但未找到文件记录");
            }
            uploadDestinationReservationService.release(task.getTaskId());
            return fileRecordGateway.findById(task.getFileId())
                    .orElseThrow(() -> new BizException("文件记录不存在: " + task.getFileId()));
        }
        if (task.getStatus() != TransferTaskStatus.uploading && task.getStatus() != TransferTaskStatus.merging) {
            throw new BizException("任务状态不支持合并: " + task.getStatus());
        }
        Path chunkDir = chunkDir(task.getTaskId());
        try {
            log.info("开始合并上传分片: taskId={}, userId={}, totalChunks={}, fileName={}",
                    task.getTaskId(), task.getUserId(), task.getTotalChunks(), task.getFileName());
            transferSseService.sendStatus(task.getUserId(), task.getTaskId(), TransferTaskStatus.merging.name(), "开始合并");
            fileTransferTaskGateway.save(task.toBuilder().status(TransferTaskStatus.merging).updatedAt(LocalDateTime.now()).build());
            Path mergedTemp = chunkDir.resolve("merged.tmp");
            Files.deleteIfExists(mergedTemp);
            Files.createFile(mergedTemp);
            for (int i = 0; i < task.getTotalChunks(); i++) {
                Path part = chunkDir.resolve(i + ".part");
                if (!Files.exists(part)) {
                    throw new BizException("分片缺失: " + i);
                }
                try (InputStream in = Files.newInputStream(part)) {
                    Files.write(mergedTemp, in.readAllBytes(), java.nio.file.StandardOpenOption.APPEND);
                }
            }
            IStorageOperationService storageService = storageServiceFacade.getStorageService(task.getStorageSettingId());
            try (InputStream mergedInput = Files.newInputStream(mergedTemp)) {
                prepareUploadDestination(task);
                storageService.uploadFile(mergedInput, task.getObjectKey());
            }
            LocalDateTime now = LocalDateTime.now();
            FileRecord fileRecord = FileRecord.builder()
                    .fileId(UUID.randomUUID().toString().replace("-", ""))
                    .objectKey(task.getObjectKey())
                    .originalName(task.getFileName())
                    .displayName(task.getFileName())
                    .suffix(task.getSuffix())
                    .size(task.getFileSize())
                    .mimeType(task.getMimeType())
                    .isDir(false)
                    .parentId(task.getParentId())
                    .userId(task.getUserId())
                    .contentMd5(task.getFileMd5())
                    .storageSettingId(task.getStorageSettingId())
                    .uploadTime(now)
                    .updateTime(now)
                    .isDeleted(false)
                    .build();
            FileRecord saved = fileRecordGateway.save(fileRecord);
            fileTransferTaskGateway.save(task.toBuilder()
                    .fileId(saved.getFileId())
                    .status(TransferTaskStatus.completed)
                    .completeTime(now)
                    .uploadedChunks(task.getTotalChunks())
                    .uploadedSize(task.getFileSize())
                    .updatedAt(now)
                    .build());
            log.info("上传任务完成: taskId={}, userId={}, fileId={}, objectKey={}",
                    task.getTaskId(), task.getUserId(), saved.getFileId(), task.getObjectKey());
            transferSseService.sendComplete(task.getUserId(), task.getTaskId(), "合并完成");
            Files.deleteIfExists(mergedTemp);
            uploadDestinationReservationService.release(task.getTaskId());
            return saved;
        } catch (Exception e) {
            log.error("分片合并失败: taskId={}, userId={}, message={}", task.getTaskId(), task.getUserId(), e.getMessage(), e);
            uploadDestinationReservationService.release(task.getTaskId());
            if (e instanceof IOException) {
                fileTransferTaskGateway.updateStatus(task.getTaskId(), TransferTaskStatus.failed, e.getMessage());
                transferSseService.sendError(task.getUserId(), task.getTaskId(), "MERGE_FAILED", e.getMessage());
                throw new BizException("分片合并失败: " + e.getMessage());
            }
            throw e instanceof RuntimeException runtimeException ? runtimeException : new BizException("分片合并失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InitDownloadResultDTO initDownload(InitDownloadCommand command) {
        command.setUserId(resolveUserId(command.getUserId()));
        FileRecord fileRecord = fileRecordGateway.findById(command.getFileId())
                .orElseThrow(() -> new BizException("文件不存在: " + command.getFileId()));
        if (!fileRecord.getUserId().equals(command.getUserId())) {
            throw new BizException("无权下载该文件");
        }
        long chunkSize = command.getChunkSize() == null || command.getChunkSize() <= 0
                ? 5L * 1024 * 1024
                : command.getChunkSize();
        int totalChunks = (int) ((fileRecord.getSize() + chunkSize - 1) / chunkSize);
        String taskId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        log.info("收到下载初始化请求: userId={}, fileId={}, fileName={}, fileSize={}, totalChunks={}, chunkSize={}",
                command.getUserId(), fileRecord.getFileId(), fileRecord.getDisplayName(), fileRecord.getSize(), totalChunks, chunkSize);
        FileTransferTask task = FileTransferTask.builder()
                .taskId(taskId)
                .uploadId(taskId)
                .userId(command.getUserId())
                .fileId(fileRecord.getFileId())
                .objectKey(fileRecord.getObjectKey())
                .fileName(fileRecord.getDisplayName())
                .fileSize(fileRecord.getSize())
                .suffix(fileRecord.getSuffix())
                .mimeType(fileRecord.getMimeType())
                .totalChunks(totalChunks)
                .chunkSize(chunkSize)
                .taskType(TransferTaskType.download)
                .uploadedChunks(0)
                .uploadedSize(0L)
                .status(TransferTaskStatus.initialized)
                .startTime(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        fileTransferTaskGateway.save(task);
        log.info("下载任务初始化完成: taskId={}, userId={}, fileId={}, objectKey={}",
                task.getTaskId(), task.getUserId(), task.getFileId(), task.getObjectKey());
        transferSseService.sendStatus(task.getUserId(), task.getTaskId(), TransferTaskStatus.initialized.name(), "任务已初始化");
        return InitDownloadResultDTO.builder()
                .taskId(taskId)
                .fileId(fileRecord.getFileId())
                .fileName(fileRecord.getDisplayName())
                .fileSize(fileRecord.getSize())
                .chunkSize(chunkSize)
                .totalChunks(totalChunks)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(String taskId) {
        FileTransferTask task = fileTransferTaskGateway.findByTaskId(taskId).orElseThrow(() -> new BizException("传输任务不存在: " + taskId));
        fileTransferTaskGateway.updateStatus(taskId, TransferTaskStatus.canceled, null);
        deleteChunkDir(taskId);
        deleteDownloadChunkDir(taskId);
        transferSseService.sendStatus(task.getUserId(), taskId, TransferTaskStatus.canceled.name(), "任务已取消");
        uploadDestinationReservationService.release(taskId);
    }

    @Override
    public void pause(String taskId) {
        FileTransferTask task = fileTransferTaskGateway.findByTaskId(taskId).orElseThrow(() -> new BizException("传输任务不存在: " + taskId));
        if (task.getStatus() == TransferTaskStatus.completed || task.getStatus() == TransferTaskStatus.canceled) {
            throw new BizException("任务已结束，无法暂停");
        }
        fileTransferTaskGateway.updateStatus(taskId, TransferTaskStatus.paused, null);
        transferSseService.sendStatus(task.getUserId(), taskId, TransferTaskStatus.paused.name(), "任务已暂停");
    }

    @Override
    public void resume(String taskId) {
        FileTransferTask task = fileTransferTaskGateway.findByTaskId(taskId).orElseThrow(() -> new BizException("传输任务不存在: " + taskId));
        if (task.getStatus() != TransferTaskStatus.paused) {
            throw new BizException("任务未处于暂停状态");
        }
        TransferTaskStatus status = task.getTaskType() == TransferTaskType.download
                ? TransferTaskStatus.downloading
                : TransferTaskStatus.uploading;
        fileTransferTaskGateway.updateStatus(taskId, status, null);
        transferSseService.sendStatus(task.getUserId(), taskId, status.name(), "任务已恢复");
    }

    @Override
    public List<FileTransferTaskDTO> listByUserId(String userId, Integer statusType) {
        userId = resolveUserId(userId);
        return fileTransferTaskGateway.listByUserId(userId, statusType).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public PageDTO<FileTransferTaskDTO> pageByUserId(String userId, Integer statusType, String keyword, long pageIndex, long pageSize) {
        userId = resolveUserId(userId);
        PageDTO<FileTransferTask> result = fileTransferTaskGateway.pageByUserId(userId, statusType, keyword, pageIndex, pageSize);
        return PageDTO.<FileTransferTaskDTO>builder()
                .total(result.getTotal())
                .pageIndex(result.getPageIndex())
                .pageSize(result.getPageSize())
                .records(result.getRecords().stream().map(this::toDTO).toList())
                .build();
    }

    @Override
    public FileTransferStatsDTO getStats(String userId) {
        userId = resolveUserId(userId);
        return FileTransferStatsDTO.builder()
                .uploadingCount(fileTransferTaskGateway.countByUserId(userId, 1))
                .downloadingCount(fileTransferTaskGateway.countByUserId(userId, 2))
                .completedCount(fileTransferTaskGateway.countByUserId(userId, 3))
                .build();
    }

    @Override
    public FileTransferTaskDTO getByTaskId(String taskId) {
        FileTransferTask task = fileTransferTaskGateway.findByTaskId(taskId).orElseThrow(() -> new BizException("传输任务不存在: " + taskId));
        return toDTO(task);
    }

    @Override
    public byte[] downloadChunk(DownloadChunkQuery query) {
        FileTransferTask task = fileTransferTaskGateway.findByTaskId(query.getTaskId())
                .orElseThrow(() -> new BizException("传输任务不存在: " + query.getTaskId()));
        if (task.getTaskType() != TransferTaskType.download) {
            throw new BizException("任务不是下载任务");
        }
        if (task.getStatus() == TransferTaskStatus.canceled
                || task.getStatus() == TransferTaskStatus.completed
                || task.getStatus() == TransferTaskStatus.failed) {
            throw new BizException("任务状态不支持下载分片: " + task.getStatus());
        }
        if (task.getStatus() == TransferTaskStatus.initialized) {
            task = fileTransferTaskGateway.save(task.toBuilder()
                    .status(TransferTaskStatus.downloading)
                    .updatedAt(LocalDateTime.now())
                    .build());
            transferSseService.sendStatus(task.getUserId(), task.getTaskId(), TransferTaskStatus.downloading.name(), "开始下载");
        } else if (task.getStatus() == TransferTaskStatus.paused) {
            throw new BizException("任务已暂停，请先恢复后再下载");
        } else if (task.getStatus() != TransferTaskStatus.downloading) {
            throw new BizException("任务状态不支持下载分片: " + task.getStatus());
        }
        log.debug("接收下载分片请求: taskId={}, userId={}, chunkIndex={}, chunkSize={}",
                task.getTaskId(), task.getUserId(), query.getChunkIndex(), task.getChunkSize());
        IStorageOperationService storageService = storageServiceFacade.getStorageService(task.getStorageSettingId());
        if (!storageService.isFileExist(task.getObjectKey())) {
            throw new BizException("文件不存在");
        }
        try {
            long start = (long) query.getChunkIndex() * task.getChunkSize();
            if (start >= task.getFileSize()) {
                throw new BizException("分片索引越界");
            }
            long end = Math.min(start + task.getChunkSize(), task.getFileSize());
            log.debug("开始读取下载分片: taskId={}, chunkIndex={}, range=[{}, {}]", task.getTaskId(), query.getChunkIndex(), start, end - 1);
            byte[] chunk;
            try (InputStream in = storageService.downloadFileRange(task.getObjectKey(), start, end - 1)) {
                chunk = in.readAllBytes();
            }
            Set<Integer> downloadedChunks = recordDownloadedChunk(task.getTaskId(), query.getChunkIndex());
            int progressed = downloadedChunks.size();
            long downloadedSize = calcDownloadedSize(task, downloadedChunks);
            TransferTaskStatus status = progressed >= task.getTotalChunks() ? TransferTaskStatus.completed : TransferTaskStatus.downloading;
            fileTransferTaskGateway.save(task.toBuilder()
                    .uploadedChunks(progressed)
                    .uploadedSize(downloadedSize)
                    .status(status)
                    .completeTime(status == TransferTaskStatus.completed ? LocalDateTime.now() : null)
                    .updatedAt(LocalDateTime.now())
                    .build());
            transferSseService.sendProgress(task.getUserId(), task.getTaskId(), downloadedSize, task.getFileSize(), progressed, task.getTotalChunks());
            if (status == TransferTaskStatus.completed) {
                deleteDownloadChunkDir(task.getTaskId());
                log.info("下载任务完成: taskId={}, userId={}, fileId={}", task.getTaskId(), task.getUserId(), task.getFileId());
                transferSseService.sendComplete(task.getUserId(), task.getTaskId(), "下载完成");
            }
            return chunk;
        } catch (IOException e) {
            log.error("下载分片失败: taskId={}, userId={}, chunkIndex={}, message={}",
                    task.getTaskId(), task.getUserId(), query.getChunkIndex(), e.getMessage(), e);
            fileTransferTaskGateway.updateStatus(task.getTaskId(), TransferTaskStatus.failed, e.getMessage());
            transferSseService.sendError(task.getUserId(), task.getTaskId(), "DOWNLOAD_FAILED", e.getMessage());
            throw new BizException("下载分片失败: " + e.getMessage());
        }
    }

    @Override
    public List<Integer> getUploadedChunks(String taskId) {
        FileTransferTask task = fileTransferTaskGateway.findByTaskId(taskId)
                .orElseThrow(() -> new BizException("传输任务不存在: " + taskId));
        if (task.getTaskType() != TransferTaskType.upload) {
            throw new BizException("任务不是上传任务");
        }
        return existingChunkIndexes(chunkDir(taskId));
    }

    @Override
    public List<Integer> getDownloadedChunks(String taskId) {
        FileTransferTask task = fileTransferTaskGateway.findByTaskId(taskId)
                .orElseThrow(() -> new BizException("传输任务不存在: " + taskId));
        if (task.getTaskType() != TransferTaskType.download) {
            throw new BizException("任务不是下载任务");
        }
        return existingChunkIndexes(downloadChunkDir(taskId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearFinished(String userId) {
        userId = resolveUserId(userId);
        List<FileTransferTask> tasks = fileTransferTaskGateway.listByUserId(userId, 3);
        if (tasks.isEmpty()) {
            return;
        }
        fileTransferTaskGateway.deleteByTaskIds(tasks.stream().map(FileTransferTask::getTaskId).toList());
        for (FileTransferTask task : tasks) {
            deleteChunkDir(task.getTaskId());
            deleteDownloadChunkDir(task.getTaskId());
        }
    }

    private FileTransferTaskDTO toDTO(FileTransferTask task) {
        return FileTransferTaskDTO.builder()
                .taskId(task.getTaskId())
                .userId(task.getUserId())
                .fileName(task.getFileName())
                .fileSize(task.getFileSize())
                .totalChunks(task.getTotalChunks())
                .uploadedChunks(task.getUploadedChunks())
                .fileMd5(task.getFileMd5())
                .status(task.getStatus() == null ? null : task.getStatus().name())
                .taskType(task.getTaskType() == null ? null : task.getTaskType().name())
                .errorMsg(task.getErrorMsg())
                .startTime(task.getStartTime())
                .completeTime(task.getCompleteTime())
                .build();
    }

    private String buildObjectKey(String userId, String taskId, String fileName) {
        return userId + "/" + taskId + "_" + fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String resolveUserId(String userId) {
        return userId == null || userId.isBlank() ? AnonymousUserContext.userId() : userId;
    }

    private String resolveStorageSettingId(String storageSettingId) {
        return storageSettingId == null || storageSettingId.isBlank()
                ? StorageUtils.LOCAL_PLATFORM_IDENTIFIER
                : storageSettingId;
    }

    private String suffix(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx < 0 ? "" : fileName.substring(idx + 1);
    }

    private List<FileRecord> listActiveSiblingFiles(String userId, String parentId, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return List.of();
        }
        return fileRecordGateway.listByUserAndParentAndDeleted(userId, parentId, false).stream()
                .filter(record -> fileName.equals(record.getDisplayName()) || fileName.equals(record.getOriginalName()))
                .toList();
    }

    private void deleteRecordsWithPhysicalCleanup(List<FileRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<String> fileIds = records.stream().map(FileRecord::getFileId).toList();
        java.util.Map<String, String> deletions = new java.util.LinkedHashMap<>();
        for (FileRecord record : records) {
            String objectKey = record.getObjectKey();
            if (objectKey == null || objectKey.isBlank()) {
                continue;
            }
            if (fileRecordGateway.countByObjectKeyExcludingIds(objectKey, fileIds) == 0) {
                deletions.putIfAbsent(objectKey, record.getStorageSettingId());
            }
        }
        fileUserFavoriteGateway.deleteByUserAndFileIds(records.get(0).getUserId(), fileIds);
        fileShareItemGateway.deleteByFileIds(fileIds);
        fileRecordGateway.deleteByIds(fileIds);
        Runnable cleanup = () -> {
            for (java.util.Map.Entry<String, String> entry : deletions.entrySet()) {
                try {
                    storageServiceFacade.getStorageService(entry.getValue()).deleteFile(entry.getKey());
                } catch (Exception ignored) {
                    // 物理删除失败不影响替换结果
                }
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cleanup.run();
                }
            });
        } else {
            cleanup.run();
        }
    }

    private void prepareUploadDestination(FileTransferTask task) {
        List<FileRecord> siblings = listActiveSiblingFiles(task.getUserId(), task.getParentId(), task.getFileName());
        if (siblings.isEmpty()) {
            return;
        }
        java.util.LinkedHashMap<String, FileRecord> recordsToDelete = new java.util.LinkedHashMap<>();
        for (FileRecord sibling : siblings) {
            if (Boolean.TRUE.equals(sibling.getIsDir())) {
                collectActiveRecursiveRecords(task.getUserId(), sibling.getFileId(), recordsToDelete);
            } else {
                recordsToDelete.putIfAbsent(sibling.getFileId(), sibling);
            }
        }
        if (recordsToDelete.isEmpty()) {
            return;
        }
        log.warn("目标目录存在同名资源，上传前执行替换: userId={}, parentId={}, fileName={}, conflictCount={}",
                task.getUserId(), task.getParentId(), task.getFileName(), recordsToDelete.size());
        deleteRecordsWithPhysicalCleanup(new java.util.ArrayList<>(recordsToDelete.values()));
    }

    private void collectActiveRecursiveRecords(String userId,
                                               String fileId,
                                               java.util.LinkedHashMap<String, FileRecord> recordsToDelete) {
        if (fileId == null || fileId.isBlank() || recordsToDelete.containsKey(fileId)) {
            return;
        }
        FileRecord current = fileRecordGateway.findById(fileId).orElse(null);
        if (current == null || !userId.equals(current.getUserId()) || Boolean.TRUE.equals(current.getIsDeleted())) {
            return;
        }
        recordsToDelete.put(fileId, current);
        if (!Boolean.TRUE.equals(current.getIsDir())) {
            return;
        }
        List<FileRecord> children = fileRecordGateway.listByUserAndParentAndDeleted(userId, fileId, false);
        for (FileRecord child : children) {
            collectActiveRecursiveRecords(userId, child.getFileId(), recordsToDelete);
        }
    }

    private boolean resolveOverwriteExisting(Boolean overwriteExisting) {
        return overwriteExisting == null || overwriteExisting;
    }

    private Path chunkDir(String taskId) {
        return Path.of(storageRoot, "chunks", taskId);
    }

    private Path downloadChunkDir(String taskId) {
        return Path.of(storageRoot, "download", "chunks", taskId);
    }

    private void deleteChunkDir(String taskId) {
        Path dir = chunkDir(taskId);
        if (!Files.exists(dir)) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            stream.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private void deleteDownloadChunkDir(String taskId) {
        Path dir = downloadChunkDir(taskId);
        if (!Files.exists(dir)) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            stream.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private List<Integer> existingChunkIndexes(Path dir) {
        if (!Files.exists(dir)) {
            return List.of();
        }
        try (var stream = Files.list(dir)) {
            return stream.filter(path -> path.getFileName().toString().endsWith(".part"))
                    .map(path -> path.getFileName().toString().replace(".part", ""))
                    .map(Integer::parseInt)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new BizException("读取分片列表失败: " + e.getMessage());
        }
    }

    private Set<Integer> recordDownloadedChunk(String taskId, int chunkIndex) throws IOException {
        Path dir = downloadChunkDir(taskId);
        Files.createDirectories(dir);
        Path marker = dir.resolve(chunkIndex + ".part");
        if (!Files.exists(marker)) {
            Files.createFile(marker);
        }
        return new HashSet<>(existingChunkIndexes(dir));
    }

    private long calcDownloadedSize(FileTransferTask task, Set<Integer> downloadedChunks) {
        long total = 0L;
        for (Integer chunkIndex : downloadedChunks) {
            total += chunkSize(task, chunkIndex);
        }
        return Math.min(total, task.getFileSize() == null ? total : task.getFileSize());
    }

    private long chunkSize(FileTransferTask task, int chunkIndex) {
        if (task.getFileSize() == null || task.getFileSize() <= 0) {
            return 0L;
        }
        long chunkSize = task.getChunkSize() == null || task.getChunkSize() <= 0 ? task.getFileSize() : task.getChunkSize();
        long start = (long) chunkIndex * chunkSize;
        if (start >= task.getFileSize()) {
            return 0L;
        }
        return Math.min(chunkSize, task.getFileSize() - start);
    }

    private int countUploadedChunks(Path chunkDir) throws IOException {
        if (!Files.exists(chunkDir)) {
            return 0;
        }
        try (var stream = Files.list(chunkDir)) {
            return (int) stream.filter(path -> path.getFileName().toString().endsWith(".part")).count();
        }
    }

    private long calcUploadedSize(Path chunkDir) throws IOException {
        if (!Files.exists(chunkDir)) {
            return 0L;
        }
        try (var stream = Files.list(chunkDir)) {
            long sum = 0L;
            for (Path p : stream.filter(path -> path.getFileName().toString().endsWith(".part")).toList()) {
                sum += Files.size(p);
            }
            return sum;
        }
    }
}
