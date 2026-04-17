package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StoragePlatformDTO {
    Long id;
    String name;
    String identifier;
    String configSchema;
    String icon;
    String link;
    Integer isDefault;
    String description;
}
