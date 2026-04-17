package com.yss.filesys.application.query;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DownloadChunkQuery {
    @NotBlank
    private String taskId;
    @NotNull
    @Min(0)
    private Integer chunkIndex;
}
