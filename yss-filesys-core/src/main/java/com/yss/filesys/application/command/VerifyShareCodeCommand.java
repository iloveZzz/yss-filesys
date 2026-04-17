package com.yss.filesys.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyShareCodeCommand {
    @NotBlank
    private String shareId;

    @NotBlank
    private String shareCode;
}
