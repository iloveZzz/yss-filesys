package com.yss.filesys.feignsdk.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 文件记录信息。
 */
@Value
@Builder
public class YssFilesysFileRecordDTO {
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
