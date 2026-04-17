package com.yss.filesys.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InitDownloadCommand {
    @NotBlank
    private String userId;
    @NotBlank
    private String fileId;
    private Long chunkSize;
}
