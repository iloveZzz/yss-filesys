package com.yss.filesys.controller;

import com.yss.filesys.application.command.UpdateStorageSettingStatusCommand;
import com.yss.filesys.application.command.UpsertStorageSettingCommand;
import com.yss.filesys.application.dto.StorageActivePlatformDTO;
import com.yss.filesys.application.dto.StoragePlatformDTO;
import com.yss.filesys.application.dto.StorageSettingDTO;
import com.yss.filesys.application.port.StorageCommandUseCase;
import com.yss.filesys.application.port.StorageQueryUseCase;
import com.yss.filesys.common.AnonymousUserContext;
import com.yss.filesys.common.MultiResult;
import com.yss.filesys.common.SingleResult;
import com.yss.filesys.domain.model.BizException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 存储平台配置控制器
 * <p>
 * 提供存储平台查询、存储配置管理等接口
 * </p>
 */
@RestController
@RequestMapping("/storage")
@Tag(name = "存储平台配置")
public class StorageController {

    /**
     * 存储查询用例
     */
    private final StorageQueryUseCase storageQueryUseCase;
    /**
     * 存储命令用例
     */
    private final StorageCommandUseCase storageCommandUseCase;

    public StorageController(StorageQueryUseCase storageQueryUseCase, StorageCommandUseCase storageCommandUseCase) {
        this.storageQueryUseCase = storageQueryUseCase;
        this.storageCommandUseCase = storageCommandUseCase;
    }

    /**
     * 查询存储平台列表
     *
     * @return 存储平台列表
     */
    @GetMapping("/platforms")
    @Operation(summary = "查询存储平台列表")
    public MultiResult<StoragePlatformDTO> listPlatforms() {
        return MultiResult.ok(storageQueryUseCase.listPlatforms());
    }

    /**
     * 根据标识符查询存储平台
     */
    @GetMapping("/platform/{identifier}")
    @Operation(summary = "根据标识符查询存储平台")
    public SingleResult<StoragePlatformDTO> getPlatformByIdentifier(@PathVariable String identifier) {
        return SingleResult.ok(storageQueryUseCase.getPlatformByIdentifier(identifier)
                .orElseThrow(() -> new BizException("存储平台不存在: " + identifier)));
    }

    /**
     * 按用户查询存储配置列表
     * @return 存储配置列表
     */
    @GetMapping("/settings")
    @Operation(summary = "按用户查询存储配置")
    public MultiResult<StorageSettingDTO> listSettings() {
        return MultiResult.ok(storageQueryUseCase.listSettingsByUser(AnonymousUserContext.userId()));
    }

    /**
     * 获取已启用存储配置列表
     */
    @GetMapping("/active-platforms")
    @Operation(summary = "获取已启用存储配置列表")
    public MultiResult<StorageActivePlatformDTO> listActivePlatforms() {
        return MultiResult.ok(storageQueryUseCase.listActivePlatforms(AnonymousUserContext.userId()));
    }

    /**
     * 新增或更新存储配置
     *
     * @param command 存储配置命令
     * @return 存储配置信息
     */
    @PostMapping("/settings")
    @Operation(summary = "新增或更新存储配置")
    public SingleResult<StorageSettingDTO> upsert(@Valid @RequestBody UpsertStorageSettingCommand command) {
        command.setUserId(AnonymousUserContext.userId());
        return SingleResult.ok(storageCommandUseCase.upsert(command));
    }

    /**
     * 启用或禁用存储配置
     *
     * @param id      配置ID
     * @param enabled 是否启用（1启用，0禁用）
     * @return 操作结果
     */
    @PutMapping("/settings/{id}/status/{enabled}")
    @Operation(summary = "启用或禁用存储配置")
    public SingleResult<Void> updateStatus(@PathVariable String id, @PathVariable Integer enabled) {
        UpdateStorageSettingStatusCommand command = new UpdateStorageSettingStatusCommand();
        command.setId(id);
        command.setEnabled(enabled);
        storageCommandUseCase.updateStatus(command);
        return SingleResult.ok();
    }

    /**
     * 删除存储配置
     */
    @DeleteMapping("/settings/{id}")
    @Operation(summary = "删除存储配置")
    public SingleResult<Void> deleteSetting(@PathVariable String id) {
        storageCommandUseCase.delete(id);
        return SingleResult.ok();
    }
}
