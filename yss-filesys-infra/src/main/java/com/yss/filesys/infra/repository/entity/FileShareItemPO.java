package com.yss.filesys.infra.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_share_items")
public class FileShareItemPO {
    @TableField("share_id")
    private String shareId;
    @TableField("file_id")
    private String fileId;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
