package com.yss.filesys.feignsdk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 合并分片请求。
 */
@Data
public class YssFilesysMergeChunksRequest {

    /**
     * 上传任务ID。
     */
    @NotBlank
    private String taskId;
}
