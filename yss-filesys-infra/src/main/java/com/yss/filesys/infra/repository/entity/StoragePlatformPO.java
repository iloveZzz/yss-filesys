package com.yss.filesys.infra.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("storage_platform")
public class StoragePlatformPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String identifier;
    @TableField("config_scheme")
    private String configScheme;
    private String icon;
    private String link;
    @TableField("is_default")
    private Integer isDefault;
    @TableField("`desc`")
    private String description;
}
