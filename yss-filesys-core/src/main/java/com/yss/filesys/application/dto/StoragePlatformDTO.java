package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

/**
 * 存储平台DTO
 * <p>
 * 用于返回存储平台的基本信息
 * </p>
 */
@Value
@Builder
public class StoragePlatformDTO {
    /**
     * 平台ID
     */
    Long id;
    /**
     * 平台名称
     */
    String name;
    /**
     * 平台标识符
     */
    String identifier;
    /**
     * 配置模式（JSON格式）
     */
    String configSchema;
    /**
     * 图标
     */
    String icon;
    /**
     * 链接地址
     */
    String link;
    /**
     * 是否为默认平台
     */
    Integer isDefault;
    /**
     * 描述信息
     */
    String description;
}
