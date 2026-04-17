package com.yss.filesys.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateDirectoryCommand {
    @NotBlank
    private String userId;
    private String parentId;
    @NotBlank
    private String folderName;
    @NotBlank
    private String storageSettingId;
}
