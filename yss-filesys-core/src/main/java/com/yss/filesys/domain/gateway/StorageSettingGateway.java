package com.yss.filesys.domain.gateway;

import com.yss.filesys.domain.model.StorageSetting;

import java.util.List;
import java.util.Optional;

public interface StorageSettingGateway {

    List<StorageSetting> listByUserId(String userId);

    Optional<StorageSetting> findById(String id);

    StorageSetting save(StorageSetting setting);

    void updateEnabled(String id, Integer enabled);
}
