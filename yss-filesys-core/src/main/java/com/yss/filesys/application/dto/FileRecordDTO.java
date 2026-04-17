package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class FileRecordDTO {
    String fileId;
    String originalName;
    String displayName;
    String suffix;
    Long size;
    Boolean isDir;
    String parentId;
    String userId;
    String storageSettingId;
    Boolean isDeleted;
    Boolean isFavorite;
    LocalDateTime uploadTime;
    LocalDateTime updateTime;
}
