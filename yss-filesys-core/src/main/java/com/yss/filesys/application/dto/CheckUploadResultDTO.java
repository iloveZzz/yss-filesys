package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CheckUploadResultDTO {
    boolean instantUpload;
    String taskId;
    String status;
    String message;
}
