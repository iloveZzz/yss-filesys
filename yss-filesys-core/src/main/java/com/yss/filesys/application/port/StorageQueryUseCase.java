package com.yss.filesys.application.port;

import com.yss.filesys.application.dto.StoragePlatformDTO;
import com.yss.filesys.application.dto.StorageSettingDTO;

import java.util.List;

public interface StorageQueryUseCase {

    List<StoragePlatformDTO> listPlatforms();

    List<StorageSettingDTO> listSettingsByUser(String userId);
}
