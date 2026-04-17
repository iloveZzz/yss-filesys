package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 文件分享访问记录DTO
 * <p>
 * 用于返回分享访问记录信息
 * </p>
 */
@Value
@Builder
public class FileShareAccessRecordDTO {
    /**
     * 记录ID
     */
    String id;
    /**
     * 分享ID
     */
    String shareId;
    /**
     * 访问IP地址
     */
    String accessIp;
    /**
     * 访问地理位置
     */
    String accessAddress;
    /**
     * 浏览器类型
     */
    String browser;
    /**
     * 操作系统
     */
    String os;
    /**
     * 访问时间
     */
    LocalDateTime accessTime;
}
