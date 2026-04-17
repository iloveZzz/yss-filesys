package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

/**
 * 上传校验结果DTO
 * <p>
 * 用于返回文件上传前的MD5校验结果
 * </p>
 */
@Value
@Builder
public class CheckUploadResultDTO {
    /**
     * 是否秒传
     */
    boolean instantUpload;
    /**
     * 任务ID
     */
    String taskId;
    /**
     * 任务状态
     */
    String status;
    /**
     * 提示信息
     */
    String message;
}
