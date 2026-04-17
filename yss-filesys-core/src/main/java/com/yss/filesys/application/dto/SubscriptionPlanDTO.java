package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class SubscriptionPlanDTO {
    Long id;
    String planCode;
    String planName;
    String description;
    Integer storageQuotaGb;
    Integer maxFiles;
    Long maxFileSize;
    Long bandwidthQuota;
    Double price;
    Integer isActive;
    Integer isDefault;
    Integer sortOrder;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
