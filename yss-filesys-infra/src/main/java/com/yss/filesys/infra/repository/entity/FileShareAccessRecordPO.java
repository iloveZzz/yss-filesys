package com.yss.filesys.infra.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_share_access_records")
public class FileShareAccessRecordPO {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    @TableField("share_id")
    private String shareId;
    @TableField("access_ip")
    private String accessIp;
    @TableField("access_address")
    private String accessAddress;
    private String browser;
    private String os;
    @TableField("access_time")
    private LocalDateTime accessTime;
}
