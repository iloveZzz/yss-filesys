package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 存储配置DTO
 * <p>
 * 用于返回存储配置信息
 * </p>
 */
@Value
@Builder
public class StorageSettingDTO {
    /**
     * 配置ID
     */
    String id;
    /**
     * 平台标识符
     */
    String platformIdentifier;
    /**
     * 配置数据（JSON格式）
     */
    String configData;
    /**
     * 是否启用
     */
    Integer enabled;
    /**
     * 所属用户ID
     */
    String userId;
    /**
     * 备注信息
     */
    String remark;
    /**
     * 创建时间
     */
    LocalDateTime createdAt;
    /**
     * 更新时间
     */
    LocalDateTime updatedAt;
}
