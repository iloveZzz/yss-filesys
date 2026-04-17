package com.yss.filesys.application.port;

import com.yss.filesys.application.command.UpdateStorageSettingStatusCommand;
import com.yss.filesys.application.command.UpsertStorageSettingCommand;
import com.yss.filesys.application.dto.StorageSettingDTO;

public interface StorageCommandUseCase {

    StorageSettingDTO upsert(UpsertStorageSettingCommand command);

    void updateStatus(UpdateStorageSettingStatusCommand command);

    void delete(String id);
}
