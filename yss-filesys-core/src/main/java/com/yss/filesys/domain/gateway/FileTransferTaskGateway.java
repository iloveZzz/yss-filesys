package com.yss.filesys.domain.gateway;

import com.yss.filesys.domain.model.FileTransferTask;
import com.yss.filesys.domain.model.TransferTaskStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FileTransferTaskGateway {

    FileTransferTask save(FileTransferTask task);

    Optional<FileTransferTask> findByTaskId(String taskId);

    List<FileTransferTask> listByUserId(String userId, Integer statusType);

    long countByUserId(String userId, Integer statusType);

    List<FileTransferTask> listFinishedBefore(LocalDateTime cutoff);

    void updateStatus(String taskId, TransferTaskStatus status, String errorMsg);

    void deleteByTaskId(String taskId);

    void deleteByTaskIds(Collection<String> taskIds);
}
