package com.yss.filesys.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder(toBuilder = true)
public class FileRecord {
    String fileId;
    String objectKey;
    String originalName;
    String displayName;
    String suffix;
    Long size;
    String mimeType;
    Boolean isDir;
    String parentId;
    String userId;
    String contentMd5;
    String storageSettingId;
    LocalDateTime uploadTime;
    LocalDateTime updateTime;
    LocalDateTime lastAccessTime;
    Boolean isDeleted;
    LocalDateTime deletedTime;
}
