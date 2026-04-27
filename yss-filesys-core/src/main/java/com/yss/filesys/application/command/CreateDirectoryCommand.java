package com.yss.filesys.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateDirectoryCommand {
    private String userId;
    private String parentId;
    @NotBlank(message = "目录名称不能为空")
    private String folderName;
    private String storageSettingId;
}
