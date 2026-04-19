package com.yss.filesys.feignsdk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 上传校验请求。
 */
@Data
public class YssFilesysCheckUploadRequest {

    /**
     * 上传任务ID。
     */
    @NotBlank
    private String taskId;

    /**
     * 文件MD5。
     */
    @NotBlank
    private String fileMd5;
}
