package com.yss.filesys.domain.gateway;

import com.yss.filesys.application.query.FileSearchQuery;
import com.yss.filesys.domain.model.FileRecord;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FileRecordGateway {

    Optional<FileRecord> findById(String fileId);

    Optional<FileRecord> findByUserAndMd5(String userId, String md5);

    List<FileRecord> search(FileSearchQuery query);

    Long count(FileSearchQuery query);

    List<FileRecord> listByUserAndParentAndDeleted(String userId, String parentId, boolean deleted);

    List<FileRecord> listByUserAndDeleted(String userId, boolean deleted);

    long countByUserAndDeleted(String userId, boolean deleted);

    List<FileRecord> listDeletedByUser(String userId);

    List<FileRecord> listDeletedBefore(LocalDateTime cutoff);

    List<FileRecord> listByIds(Collection<String> fileIds);

    long countByObjectKeyExcludingIds(String objectKey, Collection<String> excludeIds);

    FileRecord save(FileRecord fileRecord);

    void markDeleted(List<String> fileIds, boolean deleted);

    void deleteByIds(Collection<String> fileIds);
}
