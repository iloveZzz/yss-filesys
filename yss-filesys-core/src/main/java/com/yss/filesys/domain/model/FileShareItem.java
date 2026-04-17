package com.yss.filesys.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder(toBuilder = true)
public class FileShareItem {
    String shareId;
    String fileId;
    LocalDateTime createdAt;
}
