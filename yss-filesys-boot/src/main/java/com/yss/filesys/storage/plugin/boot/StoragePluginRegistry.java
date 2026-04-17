package com.yss.filesys.storage.plugin.boot;

import com.yss.filesys.storage.plugin.core.IStorageOperationService;
import com.yss.filesys.storage.plugin.core.annotation.StoragePlugin;
import com.yss.filesys.storage.plugin.core.dto.StoragePluginMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoragePluginRegistry {

    private final ApplicationContext applicationContext;
    private final Map<String, IStorageOperationService> prototypes = new HashMap<>();
    private final Map<String, StoragePluginMetadata> metadataMap = new HashMap<>();

    public synchronized void initialize() {
        prototypes.clear();
        metadataMap.clear();
        Map<String, IStorageOperationService> beans = applicationContext.getBeansOfType(IStorageOperationService.class);
        for (IStorageOperationService plugin : beans.values()) {
            Class<?> pluginClass = plugin.getClass();
            StoragePlugin annotation = pluginClass.getAnnotation(StoragePlugin.class);
            if (annotation == null) {
                continue;
            }
            StoragePluginMetadata metadata = StoragePluginMetadata.fromPluginClass(pluginClass);
            prototypes.put(metadata.getIdentifier(), plugin);
            metadataMap.put(metadata.getIdentifier(), metadata);
        }
        log.info("storage plugins initialized: {}", metadataMap.keySet());
    }

    public IStorageOperationService getPrototype(String platformIdentifier) {
        IStorageOperationService prototype = prototypes.get(platformIdentifier);
        if (prototype == null) {
            throw new IllegalArgumentException("未找到存储插件: " + platformIdentifier);
        }
        return prototype;
    }

    public Collection<StoragePluginMetadata> getAllMetadata() {
        return metadataMap.values();
    }

    public StoragePluginMetadata getMetadata(String identifier) {
        return metadataMap.get(identifier);
    }
}
