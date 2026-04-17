package com.yss.filesys.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RenameFileCommand {
    @NotBlank
    private String userId;
    @NotBlank
    private String fileName;
}
