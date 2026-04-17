package com.yss.filesys.infra.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yss.filesys.domain.gateway.FileTransferTaskGateway;
import com.yss.filesys.domain.model.FileTransferTask;
import com.yss.filesys.domain.model.TransferTaskStatus;
import com.yss.filesys.infra.repository.convertor.FileTransferTaskConvertor;
import com.yss.filesys.infra.repository.entity.FileTransferTaskPO;
import com.yss.filesys.infra.repository.mapper.FileTransferTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FileTransferTaskGatewayImpl implements FileTransferTaskGateway {

    private final FileTransferTaskMapper fileTransferTaskMapper;

    @Override
    public FileTransferTask save(FileTransferTask task) {
        FileTransferTaskPO po = FileTransferTaskConvertor.toPO(task);
        if (po.getId() == null && fileTransferTaskMapper.selectOne(new LambdaQueryWrapper<FileTransferTaskPO>()
                .eq(FileTransferTaskPO::getTaskId, po.getTaskId())) == null) {
            fileTransferTaskMapper.insert(po);
        } else {
            fileTransferTaskMapper.update(po, new LambdaUpdateWrapper<FileTransferTaskPO>()
                    .eq(FileTransferTaskPO::getTaskId, po.getTaskId()));
        }
        FileTransferTaskPO newest = fileTransferTaskMapper.selectOne(new LambdaQueryWrapper<FileTransferTaskPO>()
                .eq(FileTransferTaskPO::getTaskId, po.getTaskId()));
        return FileTransferTaskConvertor.toDomain(newest);
    }

    @Override
    public Optional<FileTransferTask> findByTaskId(String taskId) {
        FileTransferTaskPO po = fileTransferTaskMapper.selectOne(new LambdaQueryWrapper<FileTransferTaskPO>()
                .eq(FileTransferTaskPO::getTaskId, taskId));
        return po == null ? Optional.empty() : Optional.of(FileTransferTaskConvertor.toDomain(po));
    }

    @Override
    public List<FileTransferTask> listByUserId(String userId) {
        return fileTransferTaskMapper.selectList(new LambdaQueryWrapper<FileTransferTaskPO>()
                        .eq(FileTransferTaskPO::getUserId, userId)
                        .orderByDesc(FileTransferTaskPO::getCreatedAt))
                .stream()
                .map(FileTransferTaskConvertor::toDomain)
                .toList();
    }

    @Override
    public List<FileTransferTask> listFinishedBefore(LocalDateTime cutoff) {
        return fileTransferTaskMapper.selectList(new LambdaQueryWrapper<FileTransferTaskPO>()
                        .in(FileTransferTaskPO::getStatus, List.of(
                                TransferTaskStatus.completed.name(),
                                TransferTaskStatus.failed.name(),
                                TransferTaskStatus.canceled.name()))
                        .lt(FileTransferTaskPO::getCompleteTime, cutoff))
                .stream()
                .map(FileTransferTaskConvertor::toDomain)
                .toList();
    }

    @Override
    public void updateStatus(String taskId, TransferTaskStatus status, String errorMsg) {
        fileTransferTaskMapper.update(
                null,
                new LambdaUpdateWrapper<FileTransferTaskPO>()
                        .eq(FileTransferTaskPO::getTaskId, taskId)
                        .set(FileTransferTaskPO::getStatus, status.name())
                        .set(errorMsg != null, FileTransferTaskPO::getErrorMsg, errorMsg)
                        .set(FileTransferTaskPO::getUpdatedAt, LocalDateTime.now())
                        .set(status == TransferTaskStatus.completed || status == TransferTaskStatus.canceled,
                                FileTransferTaskPO::getCompleteTime, LocalDateTime.now())
        );
    }

    @Override
    public void deleteByTaskId(String taskId) {
        fileTransferTaskMapper.delete(new LambdaQueryWrapper<FileTransferTaskPO>()
                .eq(FileTransferTaskPO::getTaskId, taskId));
    }

    @Override
    public void deleteByTaskIds(Collection<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return;
        }
        fileTransferTaskMapper.delete(new LambdaQueryWrapper<FileTransferTaskPO>()
                .in(FileTransferTaskPO::getTaskId, taskIds));
    }
}
