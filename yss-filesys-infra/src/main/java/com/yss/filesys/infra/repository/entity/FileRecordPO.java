package com.yss.filesys.infra.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_info")
public class FileRecordPO {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    @TableField("object_key")
    private String objectKey;
    @TableField("original_name")
    private String originalName;
    @TableField("display_name")
    private String displayName;
    private String suffix;
    private Long size;
    @TableField("mime_type")
    private String mimeType;
    @TableField("is_dir")
    private Boolean isDir;
    @TableField("parent_id")
    private String parentId;
    @TableField("user_id")
    private String userId;
    @TableField("content_md5")
    private String contentMd5;
    @TableField("storage_platform_setting_id")
    private String storagePlatformSettingId;
    @TableField("upload_time")
    private LocalDateTime uploadTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
    @TableField("last_access_time")
    private LocalDateTime lastAccessTime;
    @TableField("is_deleted")
    private Boolean isDeleted;
    @TableField("deleted_time")
    private LocalDateTime deletedTime;
}
