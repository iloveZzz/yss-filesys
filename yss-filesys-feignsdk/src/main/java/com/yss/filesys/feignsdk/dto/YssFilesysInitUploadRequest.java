package com.yss.filesys.feignsdk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 初始化上传请求。
 */
@Data
public class YssFilesysInitUploadRequest {

    /**
     * 文件名。
     */
    @NotBlank
    private String fileName;

    /**
     * 文件大小。
     */
    @NotNull
    private Long fileSize;

    /**
     * 父目录ID。
     */
    private String parentId;

    /**
     * 总分片数。
     */
    @NotNull
    private Integer totalChunks;

    /**
     * 分片大小。
     */
    @NotNull
    private Long chunkSize;

    /**
     * MIME 类型。
     */
    @NotBlank
    private String mimeType;

    /**
     * 存储配置ID。
     */
    private String storageSettingId;
}
