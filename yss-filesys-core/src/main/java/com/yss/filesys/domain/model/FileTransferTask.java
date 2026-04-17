package com.yss.filesys.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder(toBuilder = true)
public class FileTransferTask {
    Long id;
    String taskId;
    String uploadId;
    String parentId;
    String userId;
    String storageSettingId;
    String objectKey;
    String fileId;
    String fileName;
    Long fileSize;
    String fileMd5;
    String suffix;
    String mimeType;
    Integer totalChunks;
    TransferTaskType taskType;
    Integer uploadedChunks;
    Long chunkSize;
    Long uploadedSize;
    TransferTaskStatus status;
    String errorMsg;
    LocalDateTime startTime;
    LocalDateTime completeTime;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
