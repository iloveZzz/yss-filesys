package com.yss.filesys.infra.repository.convertor;

import com.yss.filesys.domain.model.FileTransferTask;
import com.yss.filesys.domain.model.TransferTaskStatus;
import com.yss.filesys.domain.model.TransferTaskType;
import com.yss.filesys.infra.repository.entity.FileTransferTaskPO;

public final class FileTransferTaskConvertor {

    private FileTransferTaskConvertor() {
    }

    public static FileTransferTask toDomain(FileTransferTaskPO po) {
        return FileTransferTask.builder()
                .id(po.getId())
                .taskId(po.getTaskId())
                .uploadId(po.getUploadId())
                .parentId(po.getParentId())
                .userId(po.getUserId())
                .storageSettingId(po.getStoragePlatformSettingId())
                .objectKey(po.getObjectKey())
                .fileId(po.getFileId())
                .fileName(po.getFileName())
                .fileSize(po.getFileSize())
                .fileMd5(po.getFileMd5())
                .suffix(po.getSuffix())
                .mimeType(po.getMimeType())
                .totalChunks(po.getTotalChunks())
                .taskType(po.getTaskType() == null ? null : TransferTaskType.valueOf(po.getTaskType()))
                .uploadedChunks(po.getUploadedChunks())
                .chunkSize(po.getChunkSize())
                .uploadedSize(po.getUploadedSize())
                .overwriteExisting(po.getOverwriteExisting())
                .status(po.getStatus() == null ? null : TransferTaskStatus.valueOf(po.getStatus()))
                .errorMsg(po.getErrorMsg())
                .startTime(po.getStartTime())
                .completeTime(po.getCompleteTime())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    public static FileTransferTaskPO toPO(FileTransferTask domain) {
        FileTransferTaskPO po = new FileTransferTaskPO();
        po.setId(domain.getId());
        po.setTaskId(domain.getTaskId());
        po.setUploadId(domain.getUploadId());
        po.setParentId(domain.getParentId());
        po.setUserId(domain.getUserId());
        po.setStoragePlatformSettingId(domain.getStorageSettingId());
        po.setObjectKey(domain.getObjectKey());
        po.setFileId(domain.getFileId());
        po.setFileName(domain.getFileName());
        po.setFileSize(domain.getFileSize());
        po.setFileMd5(domain.getFileMd5());
        po.setSuffix(domain.getSuffix());
        po.setMimeType(domain.getMimeType());
        po.setTotalChunks(domain.getTotalChunks());
        po.setTaskType(domain.getTaskType() == null ? null : domain.getTaskType().name());
        po.setUploadedChunks(domain.getUploadedChunks());
        po.setChunkSize(domain.getChunkSize());
        po.setUploadedSize(domain.getUploadedSize());
        po.setOverwriteExisting(domain.getOverwriteExisting());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setErrorMsg(domain.getErrorMsg());
        po.setStartTime(domain.getStartTime());
        po.setCompleteTime(domain.getCompleteTime());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }
}
