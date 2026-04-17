package com.yss.filesys.application.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscriptionPlanAddCommand {

    @NotBlank(message = "planCode不能为空")
    private String planCode;

    @NotBlank(message = "planName不能为空")
    private String planName;

    private String description;

    @NotNull(message = "storageQuotaGb不能为空")
    private Integer storageQuotaGb;

    @NotNull(message = "maxFiles不能为空")
    private Integer maxFiles;

    @NotNull(message = "maxFileSize不能为空")
    private Long maxFileSize;

    @NotNull(message = "bandwidthQuota不能为空")
    private Long bandwidthQuota;

    @NotNull(message = "price不能为空")
    private Double price;

    @NotNull(message = "isActive不能为空")
    @Min(value = 0, message = "isActive只能是0或1")
    @Max(value = 1, message = "isActive只能是0或1")
    private Integer isActive;

    @NotNull(message = "isDefault不能为空")
    @Min(value = 0, message = "isDefault只能是0或1")
    @Max(value = 1, message = "isDefault只能是0或1")
    private Integer isDefault;

    @NotNull(message = "sortOrder不能为空")
    private Integer sortOrder;
}
