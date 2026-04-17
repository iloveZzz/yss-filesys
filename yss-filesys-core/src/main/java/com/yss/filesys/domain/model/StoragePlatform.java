package com.yss.filesys.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class StoragePlatform {
    Long id;
    String name;
    String identifier;
    String configSchema;
    String icon;
    String link;
    Integer isDefault;
    String description;
}
