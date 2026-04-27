package com.yss.filesys.feignsdk.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 按目录路径上传请求。
 */
@Data
public class YssFilesysUploadToDirectoryRequest {

    /**
     * 目标目录路径，支持多级目录。
     * <p>
     * 例如：<code>项目资料/2026/合同</code>。
     * </p>
     */
    private String directoryPath;

    /**
     * 存储配置ID。
     */
    private String storageSettingId;

    /**
     * 分片大小，单位字节；为空时使用默认值。
     */
    @Positive
    private Long chunkSize;

    /**
     * 是否覆盖同目录下同名文件，默认覆盖。
     * <p>
     * 传入 <code>false</code> 时，服务端会在检测到同目录同名文件时拒绝覆盖。
     * </p>
     */
    private Boolean overwriteExisting = Boolean.TRUE;
}
