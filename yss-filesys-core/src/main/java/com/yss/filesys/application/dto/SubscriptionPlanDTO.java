package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 套餐DTO
 * <p>
 * 用于返回订阅套餐的详细信息
 * </p>
 */
@Value
@Builder
public class SubscriptionPlanDTO {
    /**
     * 套餐ID
     */
    Long id;
    /**
     * 套餐编码
     */
    String planCode;
    /**
     * 套餐名称
     */
    String planName;
    /**
     * 套餐描述
     */
    String description;
    /**
     * 存储配额（GB）
     */
    Integer storageQuotaGb;
    /**
     * 最大文件数量
     */
    Integer maxFiles;
    /**
     * 单文件最大大小（字节）
     */
    Long maxFileSize;
    /**
     * 带宽配额
     */
    Long bandwidthQuota;
    /**
     * 价格
     */
    Double price;
    /**
     * 是否启用
     */
    Integer isActive;
    /**
     * 是否为默认套餐
     */
    Integer isDefault;
    /**
     * 排序值
     */
    Integer sortOrder;
    /**
     * 创建时间
     */
    LocalDateTime createdAt;
    /**
     * 更新时间
     */
    LocalDateTime updatedAt;
}
