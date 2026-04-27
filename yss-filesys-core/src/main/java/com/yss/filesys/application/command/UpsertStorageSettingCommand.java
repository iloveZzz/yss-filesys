package com.yss.filesys.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpsertStorageSettingCommand {
    private String id;
    @NotBlank
    private String platformIdentifier;
    @NotBlank
    private String configData;
    private String userId;
    private String remark;
    private Integer enabled;
}
