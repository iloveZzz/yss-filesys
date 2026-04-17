package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class StorageSettingDTO {
    String id;
    String platformIdentifier;
    String configData;
    Integer enabled;
    String userId;
    String remark;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
