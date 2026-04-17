package com.yss.filesys.application.port;

import com.yss.filesys.application.dto.StoragePlatformDTO;
import com.yss.filesys.application.dto.StorageSettingDTO;
import com.yss.filesys.application.dto.StorageActivePlatformDTO;

import java.util.List;
import java.util.Optional;

public interface StorageQueryUseCase {

    List<StoragePlatformDTO> listPlatforms();

    Optional<StoragePlatformDTO> getPlatformByIdentifier(String identifier);

    List<StorageSettingDTO> listSettingsByUser(String userId);

    List<StorageActivePlatformDTO> listActivePlatforms(String userId);
}
