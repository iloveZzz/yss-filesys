package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class FileHomeDTO {
    long fileCount;
    long directoryCount;
    long recycleCount;
    long favoriteCount;
    long totalBytes;
    List<FileHomeUsedBytesDTO> usedBytes;
}
