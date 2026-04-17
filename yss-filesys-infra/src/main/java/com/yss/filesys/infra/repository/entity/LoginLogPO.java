package com.yss.filesys.infra.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_login_log")
public class LoginLogPO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private String userId;

    @TableField("username")
    private String username;

    @TableField("login_ip")
    private String loginIp;

    @TableField("login_address")
    private String loginAddress;

    @TableField("browser")
    private String browser;

    @TableField("os")
    private String os;

    @TableField("status")
    private Integer status;

    @TableField("msg")
    private String msg;

    @TableField("login_time")
    private LocalDateTime loginTime;
}
