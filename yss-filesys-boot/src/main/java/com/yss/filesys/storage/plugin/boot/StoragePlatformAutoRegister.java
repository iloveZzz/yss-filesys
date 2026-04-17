package com.yss.filesys.storage.plugin.boot;

import com.yss.filesys.domain.model.StoragePlatform;
import com.yss.filesys.storage.plugin.core.dto.StoragePluginMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoragePlatformAutoRegister implements ApplicationRunner {

    private final StoragePluginRegistry storagePluginRegistry;
    private final com.yss.filesys.domain.gateway.StoragePlatformGateway storagePlatformGateway;

    @Override
    public void run(ApplicationArguments args) {
        try {
            storagePluginRegistry.initialize();
            syncPluginsToDatabase();
        } catch (DataAccessException e) {
            log.warn("数据库不可用，跳过插件同步: {}", e.getMessage());
        } catch (Exception e) {
            log.error("插件同步失败", e);
        }
    }

    public void syncPluginsToDatabase() {
        Collection<StoragePluginMetadata> allMetadata = storagePluginRegistry.getAllMetadata();
        for (StoragePluginMetadata metadata : allMetadata) {
            syncSinglePlugin(metadata);
        }
    }

    private void syncSinglePlugin(StoragePluginMetadata metadata) {
        StoragePlatform existing = storagePlatformGateway.findByIdentifier(metadata.getIdentifier()).orElse(null);
        if (existing == null) {
            StoragePlatform platform = StoragePlatform.builder()
                    .identifier(metadata.getIdentifier())
                    .name(metadata.getName())
                    .configSchema(validateSchema(metadata.getConfigSchema()))
                    .icon(metadata.getIcon())
                    .link(metadata.getLink())
                    .description(metadata.getDescription())
                    .isDefault(Boolean.TRUE.equals(metadata.getIsDefault()) ? 1 : 0)
                    .build();
            storagePlatformGateway.save(platform);
            return;
        }
        if (needsUpdate(existing, metadata)) {
            storagePlatformGateway.save(existing.toBuilder()
                    .name(metadata.getName())
                    .configSchema(validateSchema(metadata.getConfigSchema()))
                    .icon(metadata.getIcon())
                    .link(metadata.getLink())
                    .description(metadata.getDescription())
                    .build());
        }
    }

    private boolean needsUpdate(StoragePlatform existing, StoragePluginMetadata metadata) {
        return !Objects.equals(existing.getName(), metadata.getName())
                || !Objects.equals(existing.getConfigSchema(), validateSchema(metadata.getConfigSchema()))
                || !Objects.equals(existing.getIcon(), metadata.getIcon())
                || !Objects.equals(existing.getLink(), metadata.getLink())
                || !Objects.equals(existing.getDescription(), metadata.getDescription());
    }

    private String validateSchema(String schema) {
        return (schema == null || schema.isBlank()) ? "{}" : schema;
    }
}
