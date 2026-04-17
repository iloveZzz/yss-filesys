package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 已启用存储平台DTO
 */
@Value
@Builder
public class StorageActivePlatformDTO {
    String settingId;
    String platformIdentifier;
    String platformName;
    String platformIcon;
    String remark;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Boolean isEnabled;
}
