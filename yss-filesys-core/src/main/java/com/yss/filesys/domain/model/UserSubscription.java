package com.yss.filesys.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder(toBuilder = true)
public class UserSubscription {
    Long id;
    String userId;
    Long planId;
    Integer status;
    LocalDateTime subscriptionDate;
    LocalDateTime expireDate;
}
