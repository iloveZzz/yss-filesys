package com.yss.filesys.feignsdk.dto;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 目录树节点信息。
 * <p>
 * 用于描述文件系统中的目录层级以及可选的子节点。
 * </p>
 */
@Value
@Builder
public class YssFilesysDirectoryTreeDTO {
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
    @Singular("child")
    List<YssFilesysDirectoryTreeDTO> children;
}
