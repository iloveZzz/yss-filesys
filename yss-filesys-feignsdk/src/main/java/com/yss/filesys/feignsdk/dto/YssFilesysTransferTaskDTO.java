package com.yss.filesys.feignsdk.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 传输任务信息。
 */
@Value
@Builder
public class YssFilesysTransferTaskDTO {
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
