package com.yss.filesys.infra.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_transfer_task")
public class FileTransferTaskPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("task_id")
    private String taskId;
    @TableField("upload_id")
    private String uploadId;
    @TableField("parent_id")
    private String parentId;
    @TableField("user_id")
    private String userId;
    @TableField("storage_platform_setting_id")
    private String storagePlatformSettingId;
    @TableField("object_key")
    private String objectKey;
    @TableField("file_id")
    private String fileId;
    @TableField("file_name")
    private String fileName;
    @TableField("file_size")
    private Long fileSize;
    @TableField("file_md5")
    private String fileMd5;
    private String suffix;
    @TableField("mime_type")
    private String mimeType;
    @TableField("total_chunks")
    private Integer totalChunks;
    @TableField("task_type")
    private String taskType;
    @TableField("uploaded_chunks")
    private Integer uploadedChunks;
    @TableField("chunk_size")
    private Long chunkSize;
    @TableField("uploaded_size")
    private Long uploadedSize;
    @TableField("overwrite_existing")
    private Boolean overwriteExisting;
    private String status;
    @TableField("error_msg")
    private String errorMsg;
    @TableField("start_time")
    private LocalDateTime startTime;
    @TableField("complete_time")
    private LocalDateTime completeTime;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
