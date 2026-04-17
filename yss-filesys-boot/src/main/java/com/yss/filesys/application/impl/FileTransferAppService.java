package com.yss.filesys.application.impl;

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
import com.yss.filesys.domain.gateway.FileRecordGateway;
import com.yss.filesys.domain.gateway.FileTransferTaskGateway;
import com.yss.filesys.domain.model.BizException;
import com.yss.filesys.domain.model.FileRecord;
import com.yss.filesys.domain.model.FileTransferTask;
import com.yss.filesys.domain.model.TransferTaskStatus;
import com.yss.filesys.domain.model.TransferTaskType;
import com.yss.filesys.storage.plugin.boot.StorageServiceFacade;
import com.yss.filesys.storage.plugin.core.IStorageOperationService;
import com.yss.filesys.service.TransferSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileTransferAppService implements FileTransferCommandUseCase, FileTransferQueryUseCase {

    private final FileTransferTaskGateway fileTransferTaskGateway;
    private final FileRecordGateway fileRecordGateway;
    private final StorageServiceFacade storageServiceFacade;
    private final TransferSseService transferSseService;

    @Value("${yss.files.storage-root:/tmp/yss-filesys/storage}")
    private String storageRoot;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileTransferTaskDTO initUpload(InitTransferUploadCommand command) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        String objectKey = buildObjectKey(command.getUserId(), taskId, command.getFileName());
        FileTransferTask task = FileTransferTask.builder()
                .taskId(taskId)
                .uploadId(taskId)
                .parentId(command.getParentId())
                .userId(command.getUserId())
                .storageSettingId(command.getStorageSettingId())
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
                .status(TransferTaskStatus.initialized)
                .startTime(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        FileTransferTask saved = fileTransferTaskGateway.save(task);
        transferSseService.sendStatus(saved.getUserId(), saved.getTaskId(), saved.getStatus().name(), "任务已初始化");
        return toDTO(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckUploadResultDTO checkUpload(CheckUploadCommand command) {
        FileTransferTask task = fileTransferTaskGateway.findByTaskId(command.getTaskId())
                .orElseThrow(() -> new BizException("传输任务不存在: " + command.getTaskId()));
        if (task.getStatus() != TransferTaskStatus.initialized) {
            throw new BizException("任务状态不支持校验: " + task.getStatus());
        }
        transferSseService.sendStatus(task.getUserId(), task.getTaskId(), TransferTaskStatus.checking.name(), "开始校验");

        var existed = fileRecordGateway.findByUserAndMd5(task.getUserId(), command.getFileMd5());
        if (existed.isPresent()) {
            FileRecord source = existed.get();
            IStorageOperationService storageService = storageServiceFacade.getStorageService(source.getStorageSettingId());
            if (storageService.isFileExist(source.getObjectKey())) {
                LocalDateTime now = LocalDateTime.now();
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
                        .uploadedChunks(task.getTotalChunks())
                        .uploadedSize(task.getFileSize())
                        .status(TransferTaskStatus.completed)
                        .completeTime(now)
                        .updatedAt(now)
                        .build());
                transferSseService.sendComplete(task.getUserId(), task.getTaskId(), "命中秒传");
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
            fileTransferTaskGateway.updateStatus(task.getTaskId(), TransferTaskStatus.failed, e.getMessage());
            throw new BizException("上传分片失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileRecord mergeChunks(MergeChunksCommand command) {
        FileTransferTask task = fileTransferTaskGateway.findByTaskId(command.getTaskId())
                .orElseThrow(() -> new BizException("传输任务不存在: " + command.getTaskId()));
        if (task.getStatus() != TransferTaskStatus.uploading && task.getStatus() != TransferTaskStatus.merging) {
            throw new BizException("任务状态不支持合并: " + task.getStatus());
        }
        Path chunkDir = chunkDir(task.getTaskId());
        try {
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
            transferSseService.sendComplete(task.getUserId(), task.getTaskId(), "合并完成");
            Files.deleteIfExists(mergedTemp);
            return saved;
        } catch (IOException e) {
            fileTransferTaskGateway.updateStatus(task.getTaskId(), TransferTaskStatus.failed, e.getMessage());
            transferSseService.sendError(task.getUserId(), task.getTaskId(), "MERGE_FAILED", e.getMessage());
            throw new BizException("分片合并失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InitDownloadResultDTO initDownload(InitDownloadCommand command) {
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
                .status(TransferTaskStatus.downloading)
                .startTime(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        fileTransferTaskGateway.save(task);
        transferSseService.sendStatus(task.getUserId(), task.getTaskId(), TransferTaskStatus.downloading.name(), "开始下载");
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
        transferSseService.sendStatus(task.getUserId(), taskId, TransferTaskStatus.canceled.name(), "任务已取消");
    }

    @Override
    public List<FileTransferTaskDTO> listByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new BizException("userId 不能为空");
        }
        return fileTransferTaskGateway.listByUserId(userId).stream().map(this::toDTO).toList();
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
            byte[] chunk;
            try (InputStream in = storageService.downloadFileRange(task.getObjectKey(), start, end - 1)) {
                chunk = in.readAllBytes();
            }
            int progressed = Math.max(task.getUploadedChunks() == null ? 0 : task.getUploadedChunks(), query.getChunkIndex() + 1);
            TransferTaskStatus status = progressed >= task.getTotalChunks() ? TransferTaskStatus.completed : TransferTaskStatus.downloading;
            fileTransferTaskGateway.save(task.toBuilder()
                    .uploadedChunks(progressed)
                    .uploadedSize(Math.min(task.getFileSize(), (long) progressed * task.getChunkSize()))
                    .status(status)
                    .completeTime(status == TransferTaskStatus.completed ? LocalDateTime.now() : null)
                    .updatedAt(LocalDateTime.now())
                    .build());
            transferSseService.sendProgress(task.getUserId(), task.getTaskId(), Math.min(task.getFileSize(), (long) progressed * task.getChunkSize()), task.getFileSize(), progressed, task.getTotalChunks());
            if (status == TransferTaskStatus.completed) {
                transferSseService.sendComplete(task.getUserId(), task.getTaskId(), "下载完成");
            }
            return chunk;
        } catch (IOException e) {
            fileTransferTaskGateway.updateStatus(task.getTaskId(), TransferTaskStatus.failed, e.getMessage());
            transferSseService.sendError(task.getUserId(), task.getTaskId(), "DOWNLOAD_FAILED", e.getMessage());
            throw new BizException("下载分片失败: " + e.getMessage());
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

    private String suffix(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx < 0 ? "" : fileName.substring(idx + 1);
    }

    private Path chunkDir(String taskId) {
        return Path.of(storageRoot, "chunks", taskId);
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
