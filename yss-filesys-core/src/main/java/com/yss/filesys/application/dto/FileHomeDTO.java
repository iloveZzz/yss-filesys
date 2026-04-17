package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 文件首页DTO
 * <p>
 * 用于首页展示文件统计信息
 * </p>
 */
@Value
@Builder
public class FileHomeDTO {
    /**
     * 文件数量
     */
    long fileCount;
    /**
     * 目录数量
     */
    long directoryCount;
    /**
     * 回收站文件数量
     */
    long recycleCount;
    /**
     * 收藏数量
     */
    long favoriteCount;
    /**
     * 总字节数
     */
    long totalBytes;
    /**
     * 各存储已用字节数列表
     */
    List<FileHomeUsedBytesDTO> usedBytes;
}
