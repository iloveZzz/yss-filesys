package com.yss.filesys.application.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateStorageSettingStatusCommand {
    @NotBlank
    private String id;
    @Min(0)
    @Max(1)
    private Integer enabled;
}
