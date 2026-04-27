package com.yss.filesys.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class PermanentlyDeleteRecycleCommand {
    private String userId;

    @NotEmpty
    private List<String> fileIds;
}
