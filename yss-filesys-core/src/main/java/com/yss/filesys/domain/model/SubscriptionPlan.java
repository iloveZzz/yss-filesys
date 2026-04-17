package com.yss.filesys.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder(toBuilder = true)
public class SubscriptionPlan {
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
    Integer deleted;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
