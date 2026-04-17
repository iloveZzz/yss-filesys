package com.yss.filesys.infra.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("subscription_plan")
public class SubscriptionPlanPO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String planCode;

    private String planName;

    private String description;

    private Integer storageQuotaGb;

    private Integer maxFiles;

    private Long maxFileSize;

    private Long bandwidthQuota;

    private Double price;

    private Integer isActive;

    private Integer isDefault;

    private Integer sortOrder;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
