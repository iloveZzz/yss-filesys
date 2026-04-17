package com.yss.filesys.storage.plugin.boot;

import com.yss.filesys.storage.plugin.core.IStorageOperationService;
import com.yss.filesys.storage.plugin.core.config.StorageConfig;
import com.yss.filesys.storage.plugin.core.config.StorageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class StoragePluginManager {

    private final StorageInstanceFactory storageInstanceFactory;
    private final StorageInstanceCache storageInstanceCache = new StorageInstanceCache();
    private final LocalStorageManager localStorageManager;

    public IStorageOperationService getCurrentInstance(String configId, Supplier<StorageConfig> configLoader) {
        if (StorageUtils.isLocalConfig(configId)) {
            return getLocalInstance();
        }
        return getOrCreateInstance(configId, configLoader);
    }

    public IStorageOperationService getOrCreateInstance(String configId, Supplier<StorageConfig> configLoader) {
        if (StorageUtils.isLocalConfig(configId)) {
            return getLocalInstance();
        }
        StorageConfig config = configLoader.get();
        return storageInstanceCache.getOrCreate(config.getCacheKey(), () -> storageInstanceFactory.createInstance(config));
    }

    public IStorageOperationService getLocalInstance() {
        return localStorageManager.getLocalInstance();
    }

    public boolean hasInstance(String configId) {
        if (StorageUtils.isLocalConfig(configId)) {
            return true;
        }
        return storageInstanceCache.has(configId);
    }

    public void invalidateConfig(String configId) {
        if (StorageUtils.isLocalConfig(configId)) {
            return;
        }
        storageInstanceCache.invalidate(configId);
    }

    public void clearAll() {
        storageInstanceCache.clear();
    }
}
