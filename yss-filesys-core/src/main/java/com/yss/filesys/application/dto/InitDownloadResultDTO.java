package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

/**
 * 初始化下载结果DTO
 * <p>
 * 用于返回下载任务初始化结果
 * </p>
 */
@Value
@Builder
public class InitDownloadResultDTO {
    /**
     * 任务ID
     */
    String taskId;
    /**
     * 文件ID
     */
    String fileId;
    /**
     * 文件名
     */
    String fileName;
    /**
     * 文件大小（字节）
     */
    Long fileSize;
    /**
     * 分片大小（字节）
     */
    Long chunkSize;
    /**
     * 总分片数
     */
    Integer totalChunks;
}
