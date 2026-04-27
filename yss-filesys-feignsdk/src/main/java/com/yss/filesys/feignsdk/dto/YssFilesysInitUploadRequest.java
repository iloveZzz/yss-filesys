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
     * <p>
     * 当通过目录路径上传时，由 SDK 自动解析或创建目录后回填该字段。
     * </p>
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

    /**
     * 是否覆盖同目录下同名文件，默认覆盖。
     * <p>
     * 仅在初始化上传任务时生效，最终由后端传输任务持久化。
     * </p>
     */
    private Boolean overwriteExisting = Boolean.TRUE;
}
