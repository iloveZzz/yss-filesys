package com.yss.filesys.infra.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.filesys.application.query.FileSearchQuery;
import com.yss.filesys.domain.gateway.FileRecordGateway;
import com.yss.filesys.domain.model.FileRecord;
import com.yss.filesys.infra.repository.convertor.FileRecordConvertor;
import com.yss.filesys.infra.repository.entity.FileRecordPO;
import com.yss.filesys.infra.repository.mapper.FileRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FileRecordGatewayImpl implements FileRecordGateway {

    private final FileRecordMapper fileRecordMapper;

    @Override
    public Optional<FileRecord> findById(String fileId) {
        FileRecordPO po = fileRecordMapper.selectById(fileId);
        return po == null ? Optional.empty() : Optional.of(FileRecordConvertor.toDomain(po));
    }

    @Override
    public Optional<FileRecord> findByUserAndMd5(String userId, String md5) {
        FileRecordPO po = fileRecordMapper.selectOne(new LambdaQueryWrapper<FileRecordPO>()
                .eq(FileRecordPO::getUserId, userId)
                .eq(FileRecordPO::getContentMd5, md5)
                .eq(FileRecordPO::getIsDeleted, false)
                .orderByDesc(FileRecordPO::getUploadTime)
                .last("limit 1"));
        return po == null ? Optional.empty() : Optional.of(FileRecordConvertor.toDomain(po));
    }

    @Override
    public List<FileRecord> search(FileSearchQuery query) {
        Page<FileRecordPO> page = fileRecordMapper.selectPage(
                new Page<>(query.getPageNo(), query.getPageSize()),
                buildQueryWrapper(query)
        );
        return page.getRecords().stream().map(FileRecordConvertor::toDomain).toList();
    }

    @Override
    public Long count(FileSearchQuery query) {
        return fileRecordMapper.selectCount(buildQueryWrapper(query));
    }

    @Override
    public List<FileRecord> listByUserAndParentAndDeleted(String userId, String parentId, boolean deleted) {
        return fileRecordMapper.selectList(new LambdaQueryWrapper<FileRecordPO>()
                        .eq(FileRecordPO::getUserId, userId)
                        .eq(parentId == null, FileRecordPO::getParentId, null)
                        .eq(parentId != null, FileRecordPO::getParentId, parentId)
                        .eq(FileRecordPO::getIsDeleted, deleted)
                        .orderByDesc(FileRecordPO::getUpdateTime)
                        .orderByDesc(FileRecordPO::getUploadTime))
                .stream()
                .map(FileRecordConvertor::toDomain)
                .toList();
    }

    @Override
    public List<FileRecord> listByUserAndDeleted(String userId, boolean deleted) {
        return fileRecordMapper.selectList(new LambdaQueryWrapper<FileRecordPO>()
                        .eq(FileRecordPO::getUserId, userId)
                        .eq(FileRecordPO::getIsDeleted, deleted)
                        .orderByDesc(FileRecordPO::getUpdateTime)
                        .orderByDesc(FileRecordPO::getUploadTime))
                .stream()
                .map(FileRecordConvertor::toDomain)
                .toList();
    }

    @Override
    public long countByUserAndDeleted(String userId, boolean deleted) {
        return fileRecordMapper.selectCount(new LambdaQueryWrapper<FileRecordPO>()
                .eq(FileRecordPO::getUserId, userId)
                .eq(FileRecordPO::getIsDeleted, deleted));
    }

    @Override
    public List<FileRecord> listDeletedByUser(String userId) {
        return fileRecordMapper.selectList(new LambdaQueryWrapper<FileRecordPO>()
                        .eq(FileRecordPO::getUserId, userId)
                        .eq(FileRecordPO::getIsDeleted, true)
                        .orderByDesc(FileRecordPO::getUpdateTime)
                        .orderByDesc(FileRecordPO::getUploadTime))
                .stream()
                .map(FileRecordConvertor::toDomain)
                .toList();
    }

    @Override
    public List<FileRecord> listDeletedBefore(LocalDateTime cutoff) {
        return fileRecordMapper.selectList(new LambdaQueryWrapper<FileRecordPO>()
                        .eq(FileRecordPO::getIsDeleted, true)
                        .lt(FileRecordPO::getDeletedTime, cutoff))
                .stream()
                .map(FileRecordConvertor::toDomain)
                .toList();
    }

    @Override
    public List<FileRecord> listByIds(Collection<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }
        return fileRecordMapper.selectList(new LambdaQueryWrapper<FileRecordPO>()
                        .in(FileRecordPO::getId, fileIds))
                .stream()
                .map(FileRecordConvertor::toDomain)
                .toList();
    }

    @Override
    public long countByObjectKeyExcludingIds(String objectKey, Collection<String> excludeIds) {
        return fileRecordMapper.selectCount(new LambdaQueryWrapper<FileRecordPO>()
                .eq(FileRecordPO::getObjectKey, objectKey)
                .notIn(excludeIds != null && !excludeIds.isEmpty(), FileRecordPO::getId, excludeIds));
    }

    @Override
    public FileRecord save(FileRecord fileRecord) {
        FileRecordPO po = FileRecordConvertor.toPO(fileRecord);
        if (fileRecordMapper.selectById(po.getId()) == null) {
            fileRecordMapper.insert(po);
        } else {
            fileRecordMapper.updateById(po);
        }
        return FileRecordConvertor.toDomain(po);
    }

    @Override
    public void markDeleted(List<String> fileIds, boolean deleted) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        fileRecordMapper.update(
                null,
                new LambdaUpdateWrapper<FileRecordPO>()
                        .in(FileRecordPO::getId, fileIds)
                        .set(FileRecordPO::getIsDeleted, deleted)
                        .set(FileRecordPO::getDeletedTime, deleted ? LocalDateTime.now() : null)
                        .set(FileRecordPO::getUpdateTime, LocalDateTime.now())
        );
    }

    @Override
    public void deleteByIds(Collection<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        fileRecordMapper.delete(new LambdaQueryWrapper<FileRecordPO>()
                .in(FileRecordPO::getId, fileIds));
    }

    private LambdaQueryWrapper<FileRecordPO> buildQueryWrapper(FileSearchQuery query) {
        return new LambdaQueryWrapper<FileRecordPO>()
                .eq(FileRecordPO::getUserId, query.getUserId())
                .eq(query.getParentId() != null, FileRecordPO::getParentId, query.getParentId())
                .eq(query.getDeleted() != null, FileRecordPO::getIsDeleted, query.getDeleted())
                .in(query.getFileIds() != null && !query.getFileIds().isEmpty(), FileRecordPO::getId, query.getFileIds())
                .and(query.getKeyword() != null && !query.getKeyword().isBlank(),
                        wrapper -> wrapper.like(FileRecordPO::getOriginalName, query.getKeyword())
                                .or()
                                .like(FileRecordPO::getDisplayName, query.getKeyword()))
                .orderByDesc(FileRecordPO::getUpdateTime)
                .orderByDesc(FileRecordPO::getUploadTime);
    }
}
