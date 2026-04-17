package com.yss.filesys.infra.repository.convertor;

import com.yss.filesys.domain.model.StorageSetting;
import com.yss.filesys.infra.repository.entity.StorageSettingPO;

public final class StorageSettingConvertor {

    private StorageSettingConvertor() {
    }

    public static StorageSetting toDomain(StorageSettingPO po) {
        return StorageSetting.builder()
                .id(po.getId())
                .platformIdentifier(po.getPlatformIdentifier())
                .configData(po.getConfigData())
                .enabled(po.getEnabled())
                .userId(po.getUserId())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .remark(po.getRemark())
                .deleted(po.getDeleted())
                .build();
    }

    public static StorageSettingPO toPO(StorageSetting setting) {
        StorageSettingPO po = new StorageSettingPO();
        po.setId(setting.getId());
        po.setPlatformIdentifier(setting.getPlatformIdentifier());
        po.setConfigData(setting.getConfigData());
        po.setEnabled(setting.getEnabled());
        po.setUserId(setting.getUserId());
        po.setCreatedAt(setting.getCreatedAt());
        po.setUpdatedAt(setting.getUpdatedAt());
        po.setRemark(setting.getRemark());
        po.setDeleted(setting.getDeleted());
        return po;
    }
}
