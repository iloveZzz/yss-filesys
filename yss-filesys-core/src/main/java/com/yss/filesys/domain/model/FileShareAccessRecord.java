package com.yss.filesys.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder(toBuilder = true)
public class FileShareAccessRecord {
    String id;
    String shareId;
    String accessIp;
    String accessAddress;
    String browser;
    String os;
    LocalDateTime accessTime;
}
