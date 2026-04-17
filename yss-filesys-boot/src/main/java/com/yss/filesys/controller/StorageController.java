package com.yss.filesys.controller;

import com.yss.filesys.application.command.UpdateStorageSettingStatusCommand;
import com.yss.filesys.application.command.UpsertStorageSettingCommand;
import com.yss.filesys.application.dto.StoragePlatformDTO;
import com.yss.filesys.application.dto.StorageSettingDTO;
import com.yss.filesys.application.port.StorageCommandUseCase;
import com.yss.filesys.application.port.StorageQueryUseCase;
import com.yss.filesys.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/storage")
@Tag(name = "存储平台配置")
public class StorageController {

    private final StorageQueryUseCase storageQueryUseCase;
    private final StorageCommandUseCase storageCommandUseCase;

    public StorageController(StorageQueryUseCase storageQueryUseCase, StorageCommandUseCase storageCommandUseCase) {
        this.storageQueryUseCase = storageQueryUseCase;
        this.storageCommandUseCase = storageCommandUseCase;
    }

    @GetMapping("/platforms")
    @Operation(summary = "查询存储平台列表")
    public ApiResponse<List<StoragePlatformDTO>> listPlatforms() {
        return ApiResponse.ok(storageQueryUseCase.listPlatforms());
    }

    @GetMapping("/settings")
    @Operation(summary = "按用户查询存储配置")
    public ApiResponse<List<StorageSettingDTO>> listSettings(@RequestParam String userId) {
        return ApiResponse.ok(storageQueryUseCase.listSettingsByUser(userId));
    }

    @PostMapping("/settings")
    @Operation(summary = "新增或更新存储配置")
    public ApiResponse<StorageSettingDTO> upsert(@Valid @RequestBody UpsertStorageSettingCommand command) {
        return ApiResponse.ok(storageCommandUseCase.upsert(command));
    }

    @PutMapping("/settings/{id}/status/{enabled}")
    @Operation(summary = "启用或禁用存储配置")
    public ApiResponse<Void> updateStatus(@PathVariable String id, @PathVariable Integer enabled) {
        UpdateStorageSettingStatusCommand command = new UpdateStorageSettingStatusCommand();
        command.setId(id);
        command.setEnabled(enabled);
        storageCommandUseCase.updateStatus(command);
        return ApiResponse.ok();
    }
}
