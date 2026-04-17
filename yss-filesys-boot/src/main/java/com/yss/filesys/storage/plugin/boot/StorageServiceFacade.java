package com.yss.filesys.storage.plugin.boot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.filesys.domain.gateway.StorageSettingGateway;
import com.yss.filesys.domain.model.BizException;
import com.yss.filesys.domain.model.StorageSetting;
import com.yss.filesys.storage.plugin.core.IStorageOperationService;
import com.yss.filesys.storage.plugin.core.config.StorageConfig;
import com.yss.filesys.storage.plugin.core.config.StorageUtils;
import com.yss.filesys.storage.plugin.core.context.StoragePlatformContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageServiceFacade {

    private final StoragePluginManager storagePluginManager;
    private final StorageSettingGateway storageSettingGateway;
    private final ObjectMapper objectMapper;

    public IStorageOperationService getCurrentStorageService() {
        return getStorageService(StoragePlatformContextHolder.getConfigId());
    }

    public IStorageOperationService getStorageService(String configId) {
        if (StorageUtils.isLocalConfig(configId)) {
            return storagePluginManager.getLocalInstance();
        }
        return storagePluginManager.getOrCreateInstance(configId, () -> loadConfigFromDatabase(configId));
    }

    public void refreshInstance(String configId) {
        if (StorageUtils.isLocalConfig(configId)) {
            return;
        }
        storagePluginManager.invalidateConfig(configId);
    }

    public void removeInstance(String configId) {
        if (StorageUtils.isLocalConfig(configId)) {
            return;
        }
        storagePluginManager.invalidateConfig(configId);
    }

    private StorageConfig loadConfigFromDatabase(String configId) {
        StorageSetting settings = storageSettingGateway.findById(configId)
                .orElseThrow(() -> new BizException("未找到存储配置: " + configId));
        if (settings.getDeleted() != null && settings.getDeleted() == 1) {
            throw new BizException("存储配置已删除: " + configId);
        }
        if (settings.getEnabled() != null && settings.getEnabled() == 0) {
            throw new BizException("存储配置已禁用: " + configId);
        }
        return buildStorageConfig(settings);
    }

    private StorageConfig buildStorageConfig(StorageSetting setting) {
        Map<String, Object> properties;
        try {
            if (!StringUtils.hasText(setting.getConfigData())) {
                throw new BizException("存储平台配置数据为空: " + setting.getId());
            }
            properties = objectMapper.readValue(setting.getConfigData(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new BizException("存储平台配置解析失败: " + e.getMessage());
        }
        return StorageConfig.builder()
                .configId(setting.getId())
                .platformIdentifier(setting.getPlatformIdentifier())
                .userId(setting.getUserId())
                .properties(properties)
                .enabled(setting.getEnabled() != null && setting.getEnabled() == 1)
                .remark(setting.getRemark())
                .build();
    }
}
