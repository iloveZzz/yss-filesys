package com.yss.filesys.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder(toBuilder = true)
public class LoginLogRecord {
    Long id;
    String userId;
    String username;
    String loginIp;
    String loginAddress;
    String browser;
    String os;
    Integer status;
    String msg;
    LocalDateTime loginTime;
}
