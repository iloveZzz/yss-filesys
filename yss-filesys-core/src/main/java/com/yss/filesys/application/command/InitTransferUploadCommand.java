package com.yss.filesys.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InitTransferUploadCommand {
    private String userId;
    @NotBlank
    private String fileName;
    @NotNull
    private Long fileSize;
    private String parentId;
    @NotNull
    private Integer totalChunks;
    @NotNull
    private Long chunkSize;
    @NotBlank
    private String mimeType;
    private String storageSettingId;
    private Boolean overwriteExisting = Boolean.TRUE;
}
