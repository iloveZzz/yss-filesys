package com.yss.filesys.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClearRecycleCommand {
    @NotBlank
    private String userId;
}
