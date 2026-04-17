package com.yss.filesys.storage.plugin.boot;

import com.yss.filesys.storage.plugin.core.IStorageOperationService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Slf4j
public class StorageInstanceCache {

    private final Map<String, IStorageOperationService> cache = new ConcurrentHashMap<>();

    public IStorageOperationService getOrCreate(String cacheKey, Supplier<IStorageOperationService> creator) {
        return cache.computeIfAbsent(cacheKey, key -> creator.get());
    }

    public boolean has(String cacheKey) {
        return cache.containsKey(cacheKey);
    }

    public void invalidate(String cacheKey) {
        IStorageOperationService instance = cache.remove(cacheKey);
        if (instance != null) {
            closeInstanceSafely(instance, cacheKey);
        }
    }

    public void clear() {
        for (String cacheKey : cache.keySet()) {
            invalidate(cacheKey);
        }
    }

    private void closeInstanceSafely(IStorageOperationService instance, String cacheKey) {
        try {
            instance.close();
        } catch (IOException e) {
            log.warn("关闭存储实例失败: {}", cacheKey, e);
        }
    }
}
