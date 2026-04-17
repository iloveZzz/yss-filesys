package com.yss.filesys.application.command;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class MoveToRecycleBinCommand {
    @NotEmpty
    private List<String> fileIds;
}
