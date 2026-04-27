package com.yss.filesys.feignsdk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建目录请求。
 */
@Data
public class YssFilesysCreateDirectoryRequest {

    /**
     * 父目录ID。
     * <p>
     * 为空时表示在根目录下创建。
     * </p>
     */
    private String parentId;

    /**
     * 目录名称。
     */
    @NotBlank(message = "目录名称不能为空")
    private String folderName;

    /**
     * 存储配置ID。
     */
    private String storageSettingId;
}
