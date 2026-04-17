package com.yss.filesys.application.command;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UploadChunkCommand {
    @NotBlank
    private String taskId;
    @NotNull
    @Min(0)
    private Integer chunkIndex;
    private String chunkMd5;
}
