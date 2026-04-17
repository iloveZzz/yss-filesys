package com.yss.filesys.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckUploadCommand {
    @NotBlank
    private String taskId;
    @NotBlank
    private String fileMd5;
}
