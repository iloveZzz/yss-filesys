package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 文件记录DTO
 * <p>
 * 用于返回文件的基本信息
 * </p>
 */
@Value
@Builder
public class FileRecordDTO {
    /**
     * 文件ID
     */
    String fileId;
    /**
     * 原始文件名
     */
    String originalName;
    /**
     * 显示文件名
     */
    String displayName;
    /**
     * 文件后缀
     */
    String suffix;
    /**
     * 文件大小（字节）
     */
    Long size;
    /**
     * 是否为目录
     */
    Boolean isDir;
    /**
     * 父目录ID
     */
    String parentId;
    /**
     * 所属用户ID
     */
    String userId;
    /**
     * 存储配置ID
     */
    String storageSettingId;
    /**
     * 是否已删除
     */
    Boolean isDeleted;
    /**
     * 是否收藏
     */
    Boolean isFavorite;
    /**
     * 上传时间
     */
    LocalDateTime uploadTime;
    /**
     * 更新时间
     */
    LocalDateTime updateTime;
}
