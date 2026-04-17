package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FilePreviewDTO {
    String fileId;
    String fileName;
    String mimeType;
    String previewType;
    String streamUrl;
    Long fileSize;
}
