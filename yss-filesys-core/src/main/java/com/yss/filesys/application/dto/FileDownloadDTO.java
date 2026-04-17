package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FileDownloadDTO {
    String fileName;
    Long fileSize;
    byte[] content;
}
