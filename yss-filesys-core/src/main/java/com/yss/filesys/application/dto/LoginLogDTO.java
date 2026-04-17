package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class LoginLogDTO {
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
