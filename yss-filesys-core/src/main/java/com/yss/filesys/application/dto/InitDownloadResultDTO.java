package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InitDownloadResultDTO {
    String taskId;
    String fileId;
    String fileName;
    Long fileSize;
    Long chunkSize;
    Integer totalChunks;
}
