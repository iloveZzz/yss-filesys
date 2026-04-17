package com.yss.filesys.storage.plugin.boot;

import com.yss.filesys.storage.plugin.core.IStorageOperationService;
import com.yss.filesys.storage.plugin.local.LocalStorageOperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocalStorageManager {

    private final LocalStorageOperationService localStorageOperationService;

    public IStorageOperationService getLocalInstance() {
        return localStorageOperationService;
    }
}
