package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

/**
 * 传输任务统计DTO
 */
@Value
@Builder
public class FileTransferStatsDTO {
    /**
     * 上传中任务数
     */
    long uploadingCount;
    /**
     * 下载中任务数
     */
    long downloadingCount;
    /**
     * 已完成任务数
     */
    long completedCount;
}
