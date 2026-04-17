package com.yss.filesys.infra.repository.convertor;

import com.yss.filesys.domain.model.StoragePlatform;
import com.yss.filesys.infra.repository.entity.StoragePlatformPO;

public final class StoragePlatformConvertor {

    private StoragePlatformConvertor() {
    }

    public static StoragePlatform toDomain(StoragePlatformPO po) {
        return StoragePlatform.builder()
                .id(po.getId())
                .name(po.getName())
                .identifier(po.getIdentifier())
                .configSchema(po.getConfigScheme())
                .icon(po.getIcon())
                .link(po.getLink())
                .isDefault(po.getIsDefault())
                .description(po.getDescription())
                .build();
    }

    public static StoragePlatformPO toPO(StoragePlatform domain) {
        StoragePlatformPO po = new StoragePlatformPO();
        po.setId(domain.getId());
        po.setName(domain.getName());
        po.setIdentifier(domain.getIdentifier());
        po.setConfigScheme(domain.getConfigSchema());
        po.setIcon(domain.getIcon());
        po.setLink(domain.getLink());
        po.setIsDefault(domain.getIsDefault());
        po.setDescription(domain.getDescription());
        return po;
    }
}
