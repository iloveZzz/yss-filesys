package com.yss.filesys.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder(toBuilder = true)
public class FileShareRecord {
    String shareId;
    String userId;
    String shareName;
    String shareCode;
    LocalDateTime expireTime;
    String scope;
    Integer viewCount;
    Integer maxViewCount;
    Integer downloadCount;
    Integer maxDownloadCount;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
