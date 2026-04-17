package com.yss.filesys.infra.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_shares")
public class FileSharePO {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    @TableField("user_id")
    private String userId;
    @TableField("share_name")
    private String shareName;
    @TableField("share_code")
    private String shareCode;
    @TableField("expire_time")
    private LocalDateTime expireTime;
    private String scope;
    @TableField("view_count")
    private Integer viewCount;
    @TableField("max_view_count")
    private Integer maxViewCount;
    @TableField("download_count")
    private Integer downloadCount;
    @TableField("max_download_count")
    private Integer maxDownloadCount;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
