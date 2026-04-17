package com.yss.filesys.infra.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("storage_settings")
public class StorageSettingPO {
    @TableId(type = IdType.INPUT)
    private String id;
    @TableField("platform_identifier")
    private String platformIdentifier;
    @TableField("config_data")
    private String configData;
    private Integer enabled;
    @TableField("user_id")
    private String userId;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    private String remark;
    @TableLogic
    private Integer deleted;
}
