package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

/**
 * 存储容量统计DTO
 */
@Value
@Builder
public class StorageCapacityDTO {
    /**
     * 存储配置ID
     */
    String settingId;
    /**
     * 平台标识符
     */
    String platformIdentifier;
    /**
     * 总容量，字节
     */
    long totalBytes;
    /**
     * 已使用容量，字节
     */
    long usedBytes;
    /**
     * 可用容量，字节
     */
    long freeBytes;
    /**
     * 操作系统可用容量，字节
     */
    long usableBytes;
    /**
     * 存储根目录
     */
    String storageRoot;
}
