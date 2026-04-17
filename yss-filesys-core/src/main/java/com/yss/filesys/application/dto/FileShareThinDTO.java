package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class FileShareThinDTO {
    String shareId;
    String shareName;
    String shareCode;
    Integer fileCount;
    Boolean hasCheckCode;
    Boolean isExpire;
    LocalDateTime expireTime;
    Integer viewCount;
    Integer downloadCount;
}
