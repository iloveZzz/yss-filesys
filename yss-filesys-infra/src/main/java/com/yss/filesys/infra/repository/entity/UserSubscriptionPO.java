package com.yss.filesys.infra.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_subscription")
public class UserSubscriptionPO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String userId;

    private Long planId;

    private Integer status;

    private LocalDateTime subscriptionDate;

    private LocalDateTime expireDate;
}
