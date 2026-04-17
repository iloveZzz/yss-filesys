package com.yss.filesys.storage.plugin.core.dto;

import com.yss.filesys.storage.plugin.core.annotation.StoragePlugin;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StoragePluginMetadata {
    String identifier;
    String name;
    String configSchema;
    String icon;
    String link;
    String description;
    Boolean isDefault;

    public static StoragePluginMetadata fromPluginClass(Class<?> pluginClass) {
        StoragePlugin annotation = pluginClass.getAnnotation(StoragePlugin.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Plugin class must be annotated with @StoragePlugin: " + pluginClass.getName());
        }
        return StoragePluginMetadata.builder()
                .identifier(annotation.identifier())
                .name(annotation.name())
                .configSchema(annotation.configSchema().isBlank() ? "{}" : annotation.configSchema())
                .icon(annotation.icon())
                .link(annotation.link().isBlank() ? null : annotation.link())
                .description(annotation.description().isBlank() ? annotation.name() : annotation.description())
                .isDefault(annotation.isDefault())
                .build();
    }
}
