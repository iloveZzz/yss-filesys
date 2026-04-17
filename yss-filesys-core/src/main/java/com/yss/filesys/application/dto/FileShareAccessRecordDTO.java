package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class FileShareAccessRecordDTO {
    String id;
    String shareId;
    String accessIp;
    String accessAddress;
    String browser;
    String os;
    LocalDateTime accessTime;
}
