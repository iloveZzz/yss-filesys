package com.yss.filesys.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder(toBuilder = true)
public class StorageSetting {
    String id;
    String platformIdentifier;
    String configData;
    Integer enabled;
    String userId;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String remark;
    Integer deleted;
}
