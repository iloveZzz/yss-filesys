package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

/**
 * 文件下载DTO
 * <p>
 * 用于文件下载时返回文件名、大小和内容
 * </p>
 */
@Value
@Builder
public class FileDownloadDTO {
    /**
     * 文件名
     */
    String fileName;
    /**
     * 文件大小（字节）
     */
    Long fileSize;
    /**
     * 文件内容
     */
    byte[] content;
}
