package com.yss.filesys.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateShareCommand {
    private String userId;
    @NotEmpty
    private List<String> fileIds;
    private String shareName;
    private Integer expireType;
    private LocalDateTime expireTime;
    private String scope;
    private Boolean needShareCode;
    private Integer maxViewCount;
    private Integer maxDownloadCount;
}
