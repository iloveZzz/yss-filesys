package com.yss.filesys.domain.gateway;

import com.yss.filesys.domain.model.FileTransferTask;
import com.yss.filesys.domain.model.TransferTaskStatus;
import com.yss.filesys.application.dto.PageDTO;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FileTransferTaskGateway {

    FileTransferTask save(FileTransferTask task);

    Optional<FileTransferTask> findByTaskId(String taskId);

    List<FileTransferTask> listByUserId(String userId, Integer statusType);

    long countByUserId(String userId, Integer statusType);

    PageDTO<FileTransferTask> pageByUserId(String userId, Integer statusType, String keyword, long pageIndex, long pageSize);

    List<FileTransferTask> listFinishedBefore(LocalDateTime cutoff);

    void updateStatus(String taskId, TransferTaskStatus status, String errorMsg);

    void deleteByTaskId(String taskId);

    void deleteByTaskIds(Collection<String> taskIds);
}
