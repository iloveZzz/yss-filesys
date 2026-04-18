package com.yss.filesys.application.impl;

import com.yss.filesys.application.command.UpdateStorageSettingStatusCommand;
import com.yss.filesys.application.command.UpsertStorageSettingCommand;
import com.yss.filesys.application.dto.StorageActivePlatformDTO;
import com.yss.filesys.application.dto.StorageCapacityDTO;
import com.yss.filesys.application.dto.StoragePlatformDTO;
import com.yss.filesys.application.dto.StorageSettingDTO;
import com.yss.filesys.application.port.StorageCommandUseCase;
import com.yss.filesys.application.port.StorageQueryUseCase;
import com.yss.filesys.common.AnonymousUserContext;
import com.yss.filesys.domain.gateway.StoragePlatformGateway;
import com.yss.filesys.domain.gateway.StorageSettingGateway;
import com.yss.filesys.domain.model.BizException;
import com.yss.filesys.domain.model.StoragePlatform;
import com.yss.filesys.domain.model.StorageSetting;
import com.yss.filesys.storage.plugin.boot.StorageServiceFacade;
import com.yss.filesys.application.form.StorageFormTemplateRegistry;
import com.yss.filesys.storage.plugin.core.IStorageOperationService;
import com.yss.filesys.storage.plugin.core.config.StorageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StorageAppService implements StorageCommandUseCase, StorageQueryUseCase {

    private final StoragePlatformGateway storagePlatformGateway;
    private final StorageSettingGateway storageSettingGateway;
    private final StorageServiceFacade storageServiceFacade;
    private final StorageFormTemplateRegistry storageFormTemplateRegistry;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StorageSettingDTO upsert(UpsertStorageSettingCommand command) {
        storagePlatformGateway.findByIdentifier(command.getPlatformIdentifier())
                .orElseThrow(() -> new BizException("未找到存储平台插件: " + command.getPlatformIdentifier()));
        LocalDateTime now = LocalDateTime.now();
        String id = command.getId() == null || command.getId().isBlank()
                ? UUID.randomUUID().toString().replace("-", "")
                : command.getId();
        StorageSetting current = storageSettingGateway.findById(id).orElse(null);
        StorageSetting setting = StorageSetting.builder()
                .id(id)
                .platformIdentifier(command.getPlatformIdentifier())
                .configData(command.getConfigData())
                .enabled(command.getEnabled() == null ? 0 : command.getEnabled())
                .userId(resolveUserId(command.getUserId()))
                .createdAt(current == null ? now : current.getCreatedAt())
                .updatedAt(now)
                .remark(command.getRemark())
                .deleted(0)
                .build();
        return toDTO(storageSettingGateway.save(setting));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(UpdateStorageSettingStatusCommand command) {
        storageSettingGateway.findById(command.getId()).orElseThrow(() -> new BizException("存储配置不存在: " + command.getId()));
        storageSettingGateway.updateEnabled(command.getId(), command.getEnabled());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        storageSettingGateway.findById(id).orElseThrow(() -> new BizException("存储配置不存在: " + id));
        storageSettingGateway.deleteById(id);
    }

    @Override
    public java.util.List<StoragePlatformDTO> listPlatforms() {
        return storagePlatformGateway.listAll().stream().map(this::toDTO).toList();
    }

    @Override
    public Optional<StoragePlatformDTO> getPlatformByIdentifier(String identifier) {
        return storagePlatformGateway.findByIdentifier(identifier).map(this::toDTO);
    }

    @Override
    public java.util.List<StorageSettingDTO> listSettingsByUser(String userId) {
        userId = resolveUserId(userId);
        List<StorageSettingDTO> settings = storageSettingGateway.listByUserId(userId).stream().map(this::toDTO).toList();
        if (!settings.isEmpty()) {
            return settings;
        }
        return List.of(buildLocalSettingDTO(userId));
    }

    @Override
    public java.util.List<StorageActivePlatformDTO> listActivePlatforms(String userId) {
        userId = resolveUserId(userId);
        List<StorageActivePlatformDTO> active = storageSettingGateway.listEnabledByUserId(userId).stream().map(setting -> {
            StoragePlatform platform = storagePlatformGateway.findByIdentifier(setting.getPlatformIdentifier()).orElse(null);
            return StorageActivePlatformDTO.builder()
                    .settingId(setting.getId())
                    .platformIdentifier(setting.getPlatformIdentifier())
                    .platformName(platform == null ? null : platform.getName())
                    .platformIcon(platform == null ? null : platform.getIcon())
                    .remark(setting.getRemark())
                    .createdAt(setting.getCreatedAt())
                    .updatedAt(setting.getUpdatedAt())
                    .isEnabled(setting.getEnabled() != null && setting.getEnabled() == 1)
                    .build();
        }).toList();
        if (!active.isEmpty()) {
            return active;
        }
        return List.of(buildLocalActivePlatformDTO());
    }

    @Override
    public StorageCapacityDTO getCapacity(String settingId) {
        IStorageOperationService storageService = StorageUtils.isLocalConfig(settingId)
                ? storageServiceFacade.getCurrentStorageService()
                : storageServiceFacade.getStorageService(settingId);
        return storageService.getCapacity();
    }

    private String resolveUserId(String userId) {
        return userId == null || userId.isBlank() ? AnonymousUserContext.userId() : userId;
    }

    private StoragePlatformDTO toDTO(StoragePlatform platform) {
        return StoragePlatformDTO.builder()
                .id(platform.getId())
                .name(platform.getName())
                .identifier(platform.getIdentifier())
                .configSchema(storageFormTemplateRegistry.getSchemaJson(platform.getIdentifier())
                        .orElse(platform.getConfigSchema()))
                .icon(platform.getIcon())
                .link(platform.getLink())
                .isDefault(platform.getIsDefault())
                .description(platform.getDescription())
                .build();
    }

    private StorageSettingDTO toDTO(StorageSetting setting) {
        return StorageSettingDTO.builder()
                .id(setting.getId())
                .platformIdentifier(setting.getPlatformIdentifier())
                .configData(setting.getConfigData())
                .enabled(setting.getEnabled())
                .userId(setting.getUserId())
                .remark(setting.getRemark())
                .createdAt(setting.getCreatedAt())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }

    private StorageSettingDTO buildLocalSettingDTO(String userId) {
        LocalDateTime now = LocalDateTime.now();
        return StorageSettingDTO.builder()
                .id(com.yss.filesys.storage.plugin.core.config.StorageUtils.LOCAL_PLATFORM_IDENTIFIER)
                .platformIdentifier(com.yss.filesys.storage.plugin.core.config.StorageUtils.LOCAL_PLATFORM_IDENTIFIER)
                .configData("{\"storageRoot\":\"/tmp/yss-filesys/storage\"}")
                .enabled(1)
                .userId(userId)
                .remark("default local storage")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private StorageActivePlatformDTO buildLocalActivePlatformDTO() {
        LocalDateTime now = LocalDateTime.now();
        StoragePlatform platform = storagePlatformGateway.findByIdentifier(com.yss.filesys.storage.plugin.core.config.StorageUtils.LOCAL_PLATFORM_IDENTIFIER)
                .orElse(null);
        return StorageActivePlatformDTO.builder()
                .settingId(com.yss.filesys.storage.plugin.core.config.StorageUtils.LOCAL_PLATFORM_IDENTIFIER)
                .platformIdentifier(com.yss.filesys.storage.plugin.core.config.StorageUtils.LOCAL_PLATFORM_IDENTIFIER)
                .platformName(platform == null ? "本地存储" : platform.getName())
                .platformIcon(platform == null ? "folder" : platform.getIcon())
                .remark("default local storage")
                .createdAt(now)
                .updatedAt(now)
                .isEnabled(true)
                .build();
    }
}
