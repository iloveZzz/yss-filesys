package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

/**
 * 文件首页已用字节数DTO
 * <p>
 * 用于首页展示各存储的已用空间
 * </p>
 */
@Value
@Builder
public class FileHomeUsedBytesDTO {
    /**
     * 存储名称
     */
    String label;
    /**
     * 已用字节数
     */
    long usedBytes;
}
