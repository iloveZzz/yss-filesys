package com.yss.filesys.feignsdk.dto;

import lombok.Builder;
import lombok.Value;

/**
 * 上传校验结果。
 */
@Value
@Builder
public class YssFilesysCheckUploadResultDTO {
    boolean instantUpload;
    String taskId;
    String status;
    String message;
}
