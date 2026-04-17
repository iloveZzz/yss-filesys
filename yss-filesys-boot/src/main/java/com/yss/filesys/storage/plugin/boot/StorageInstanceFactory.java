package com.yss.filesys.storage.plugin.boot;

import com.yss.filesys.storage.plugin.core.IStorageOperationService;
import com.yss.filesys.storage.plugin.core.config.StorageConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StorageInstanceFactory {

    private final StoragePluginRegistry storagePluginRegistry;

    public IStorageOperationService createInstance(StorageConfig config) {
        IStorageOperationService prototype = storagePluginRegistry.getPrototype(config.getPlatformIdentifier());
        return prototype.createConfiguredInstance(config);
    }
}
