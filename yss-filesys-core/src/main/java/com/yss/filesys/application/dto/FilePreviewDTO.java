package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

/**
 * 文件预览DTO
 * <p>
 * 用于文件预览时返回预览信息
 * </p>
 */
@Value
@Builder
public class FilePreviewDTO {
    /**
     * 文件ID
     */
    String fileId;
    /**
     * 文件名
     */
    String fileName;
    /**
     * MIME类型
     */
    String mimeType;
    /**
     * 预览类型（image/video/audio/document/markdown/code/text）
     */
    String previewType;
    /**
     * 流式传输URL
     */
    String streamUrl;
    /**
     * 文件大小（字节）
     */
    Long fileSize;
}
