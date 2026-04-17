package com.yss.filesys.infra.repository.convertor;

import com.yss.filesys.domain.model.FileRecord;
import com.yss.filesys.infra.repository.entity.FileRecordPO;

public final class FileRecordConvertor {

    private FileRecordConvertor() {
    }

    public static FileRecord toDomain(FileRecordPO po) {
        return FileRecord.builder()
                .fileId(po.getId())
                .objectKey(po.getObjectKey())
                .originalName(po.getOriginalName())
                .displayName(po.getDisplayName())
                .suffix(po.getSuffix())
                .size(po.getSize())
                .mimeType(po.getMimeType())
                .isDir(po.getIsDir())
                .parentId(po.getParentId())
                .userId(po.getUserId())
                .contentMd5(po.getContentMd5())
                .storageSettingId(po.getStoragePlatformSettingId())
                .uploadTime(po.getUploadTime())
                .updateTime(po.getUpdateTime())
                .lastAccessTime(po.getLastAccessTime())
                .isDeleted(po.getIsDeleted())
                .deletedTime(po.getDeletedTime())
                .build();
    }

    public static FileRecordPO toPO(FileRecord domain) {
        FileRecordPO po = new FileRecordPO();
        po.setId(domain.getFileId());
        po.setObjectKey(domain.getObjectKey());
        po.setOriginalName(domain.getOriginalName());
        po.setDisplayName(domain.getDisplayName());
        po.setSuffix(domain.getSuffix());
        po.setSize(domain.getSize());
        po.setMimeType(domain.getMimeType());
        po.setIsDir(domain.getIsDir());
        po.setParentId(domain.getParentId());
        po.setUserId(domain.getUserId());
        po.setContentMd5(domain.getContentMd5());
        po.setStoragePlatformSettingId(domain.getStorageSettingId());
        po.setUploadTime(domain.getUploadTime());
        po.setUpdateTime(domain.getUpdateTime());
        po.setLastAccessTime(domain.getLastAccessTime());
        po.setIsDeleted(domain.getIsDeleted());
        po.setDeletedTime(domain.getDeletedTime());
        return po;
    }
}
