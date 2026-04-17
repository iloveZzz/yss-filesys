package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件分享DTO
 * <p>
 * 用于返回文件分享的详细信息
 * </p>
 */
@Value
@Builder
public class FileShareDTO {
    /**
     * 分享ID
     */
    String shareId;
    /**
     * 所属用户ID
     */
    String userId;
    /**
     * 分享名称
     */
    String shareName;
    /**
     * 提取码
     */
    String shareCode;
    /**
     * 分享范围
     */
    String scope;
    /**
     * 过期时间
     */
    LocalDateTime expireTime;
    /**
     * 已浏览次数
     */
    Integer viewCount;
    /**
     * 最大浏览次数
     */
    Integer maxViewCount;
    /**
     * 已下载次数
     */
    Integer downloadCount;
    /**
     * 最大下载次数
     */
    Integer maxDownloadCount;
    /**
     * 分享的文件ID列表
     */
    List<String> fileIds;
    /**
     * 创建时间
     */
    LocalDateTime createdAt;
    /**
     * 更新时间
     */
    LocalDateTime updatedAt;
}
