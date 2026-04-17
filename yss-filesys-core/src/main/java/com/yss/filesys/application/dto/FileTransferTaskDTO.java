package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class FileTransferTaskDTO {
    String taskId;
    String userId;
    String fileName;
    Long fileSize;
    Integer totalChunks;
    Integer uploadedChunks;
    String fileMd5;
    String status;
    String taskType;
    String errorMsg;
    LocalDateTime startTime;
    LocalDateTime completeTime;
}
