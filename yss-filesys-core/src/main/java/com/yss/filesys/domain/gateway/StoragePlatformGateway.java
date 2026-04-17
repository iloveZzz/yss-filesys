package com.yss.filesys.domain.gateway;

import com.yss.filesys.domain.model.StoragePlatform;

import java.util.List;
import java.util.Optional;

public interface StoragePlatformGateway {

    List<StoragePlatform> listAll();

    Optional<StoragePlatform> findByIdentifier(String identifier);

    Optional<StoragePlatform> findById(Long id);

    StoragePlatform save(StoragePlatform platform);

    void deleteById(Long id);
}
