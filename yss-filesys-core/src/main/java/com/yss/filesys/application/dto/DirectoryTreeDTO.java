package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 目录树节点DTO
 */
@Value
@Builder
public class DirectoryTreeDTO {
    String fileId;
    String originalName;
    String displayName;
    String suffix;
    Long size;
    Boolean isDir;
    String parentId;
    String userId;
    String storageSettingId;
    Boolean isDeleted;
    Boolean isFavorite;
    LocalDateTime uploadTime;
    LocalDateTime updateTime;
    @Builder.Default
    List<DirectoryTreeDTO> children = List.of();
}
