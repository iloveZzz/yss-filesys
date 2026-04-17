package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 文件传输任务DTO
 * <p>
 * 用于返回文件上传/下载任务的信息
 * </p>
 */
@Value
@Builder
public class FileTransferTaskDTO {
    /**
     * 任务ID
     */
    String taskId;
    /**
     * 用户ID
     */
    String userId;
    /**
     * 文件名
     */
    String fileName;
    /**
     * 文件大小（字节）
     */
    Long fileSize;
    /**
     * 总分片数
     */
    Integer totalChunks;
    /**
     * 已传输分片数
     */
    Integer uploadedChunks;
    /**
     * 文件MD5
     */
    String fileMd5;
    /**
     * 任务状态
     */
    String status;
    /**
     * 任务类型（upload/download）
     */
    String taskType;
    /**
     * 错误信息
     */
    String errorMsg;
    /**
     * 开始时间
     */
    LocalDateTime startTime;
    /**
     * 完成时间
     */
    LocalDateTime completeTime;
}
