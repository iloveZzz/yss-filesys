package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 文件分享精简DTO
 * <p>
 * 用于分享页面展示的精简分享信息
 * </p>
 */
@Value
@Builder
public class FileShareThinDTO {
    /**
     * 分享ID
     */
    String shareId;
    /**
     * 分享名称
     */
    String shareName;
    /**
     * 提取码
     */
    String shareCode;
    /**
     * 文件数量
     */
    Integer fileCount;
    /**
     * 是否需要提取码
     */
    Boolean hasCheckCode;
    /**
     * 是否已过期
     */
    Boolean isExpire;
    /**
     * 过期时间
     */
    LocalDateTime expireTime;
    /**
     * 已浏览次数
     */
    Integer viewCount;
    /**
     * 已下载次数
     */
    Integer downloadCount;
}
