package com.yss.filesys.feignsdk.dto;

import lombok.Builder;
import lombok.Value;

/**
 * 整体上传流程结果。
 * <p>
 * 描述一次上传从初始化、校验到最终合并完成后的关键返回信息。
 * </p>
 */
@Value
@Builder
public class YssFilesysUploadFlowResult {

    /**
     * 传输任务ID。
     */
    String taskId;

    /**
     * 是否秒传。
     */
    boolean instantUpload;

    /**
     * 初始化结果。
     */
    YssFilesysTransferTaskDTO transferTask;

    /**
     * 校验结果。
     */
    YssFilesysCheckUploadResultDTO checkResult;

    /**
     * 最终文件记录。
     */
    YssFilesysFileRecordDTO fileRecord;
}
