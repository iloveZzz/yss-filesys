package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class FileShareDTO {
    String shareId;
    String userId;
    String shareName;
    String shareCode;
    String scope;
    LocalDateTime expireTime;
    Integer viewCount;
    Integer maxViewCount;
    Integer downloadCount;
    Integer maxDownloadCount;
    List<String> fileIds;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
