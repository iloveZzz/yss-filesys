package com.yss.filesys.storage.plugin.core;

import com.yss.filesys.domain.model.BizException;
import com.yss.filesys.storage.plugin.core.annotation.StoragePlugin;
import com.yss.filesys.storage.plugin.core.config.StorageConfig;

import java.lang.reflect.Constructor;
import java.util.Objects;

public abstract class AbstractStorageOperationService implements IStorageOperationService {

    protected final StorageConfig config;

    protected AbstractStorageOperationService(StorageConfig config) {
        this.config = Objects.requireNonNull(config, "StorageConfig cannot be null");
        config.validate();
        validateConfig(config);
        initialize(config);
    }

    protected AbstractStorageOperationService() {
        this.config = null;
    }

    protected abstract void validateConfig(StorageConfig config);

    protected abstract void initialize(StorageConfig config);

    @Override
    public IStorageOperationService createConfiguredInstance(StorageConfig config) {
        try {
            Constructor<? extends IStorageOperationService> constructor =
                    this.getClass().getConstructor(StorageConfig.class);
            return constructor.newInstance(config);
        } catch (Exception e) {
            throw new BizException("创建存储实例失败: " + getIdentifierFromAnnotation());
        }
    }

    protected String getIdentifierFromAnnotation() {
        StoragePlugin annotation = this.getClass().getAnnotation(StoragePlugin.class);
        return annotation == null ? "unknown" : annotation.identifier();
    }

    protected void ensureNotPrototype() {
        if (config == null) {
            throw new BizException("原型实例不能执行业务方法");
        }
    }
}
