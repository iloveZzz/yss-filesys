package com.yss.filesys.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MoveFileCommand {
    private String userId;
    @NotBlank
    private String fileId;
    private String targetParentId;
}
