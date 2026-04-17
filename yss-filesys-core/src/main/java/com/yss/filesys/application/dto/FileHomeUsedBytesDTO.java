package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FileHomeUsedBytesDTO {
    String label;
    long usedBytes;
}
