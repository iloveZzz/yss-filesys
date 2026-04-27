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
                new Page<>(query.getPageIndex() + 1, query.getPageSize()),
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
        LambdaQueryWrapper<FileRecordPO> wrapper = new LambdaQueryWrapper<FileRecordPO>()
                .eq(FileRecordPO::getUserId, query.getUserId())
                .in(query.getFileIds() != null && !query.getFileIds().isEmpty(), FileRecordPO::getId, query.getFileIds());
        if (!Boolean.TRUE.equals(query.getIsRecents())) {
            boolean isTypeFilter = query.getFileType() != null && !query.getFileType().isBlank();
            boolean isFavoriteView = Boolean.TRUE.equals(query.getFavorite()) && query.getParentId() == null;
            boolean isDirFilter = Boolean.TRUE.equals(query.getIsDir()) && query.getParentId() == null;
            boolean isRecycleView = Boolean.TRUE.equals(query.getDeleted());
            if (!isTypeFilter && !isFavoriteView && !isDirFilter && !isRecycleView) {
                if (query.getParentId() == null) {
                    wrapper.isNull(FileRecordPO::getParentId);
                } else {
                    wrapper.eq(FileRecordPO::getParentId, query.getParentId());
                }
            } else if (!isTypeFilter && !isFavoriteView && !isDirFilter && query.getParentId() != null) {
                wrapper.eq(FileRecordPO::getParentId, query.getParentId());
            }
            wrapper.eq(FileRecordPO::getIsDeleted, query.getDeleted());
        } else {
            wrapper.eq(FileRecordPO::getIsDeleted, false)
                    .eq(FileRecordPO::getIsDir, false)
                    .isNotNull(FileRecordPO::getLastAccessTime);
        }
        if (query.getIsDir() != null) {
            wrapper.eq(FileRecordPO::getIsDir, query.getIsDir());
        }
        wrapper.and(query.getKeyword() != null && !query.getKeyword().isBlank(),
                inner -> inner.like(FileRecordPO::getOriginalName, query.getKeyword())
                        .or()
                        .like(FileRecordPO::getDisplayName, query.getKeyword()));
        applyFileTypeFilter(wrapper, query);
        return applySort(wrapper, query);
    }

    private LambdaQueryWrapper<FileRecordPO> applySort(LambdaQueryWrapper<FileRecordPO> wrapper, FileSearchQuery query) {
        String sortField = normalizeSortField(query.getSortField());
        String sortOrder = normalizeSortOrder(query.getSortOrder());
        boolean asc = "asc".equals(sortOrder);
        if ("size".equals(sortField)) {
            return wrapper.orderBy(true, asc, FileRecordPO::getSize)
                    .orderByDesc(FileRecordPO::getUpdateTime)
                    .orderByDesc(FileRecordPO::getUploadTime);
        }
        if ("updateTime".equals(sortField)) {
            return wrapper.orderBy(true, asc, FileRecordPO::getUpdateTime)
                    .orderByDesc(FileRecordPO::getUploadTime);
        }
        if (Boolean.TRUE.equals(query.getIsRecents())) {
            return wrapper.orderByDesc(FileRecordPO::getLastAccessTime)
                    .orderByDesc(FileRecordPO::getUpdateTime)
                    .orderByDesc(FileRecordPO::getUploadTime);
        }
        return wrapper.orderByDesc(FileRecordPO::getUpdateTime)
                .orderByDesc(FileRecordPO::getUploadTime);
    }

    private String normalizeSortField(String sortField) {
        if (sortField == null) {
            return "";
        }
        String normalized = sortField.trim();
        if ("size".equalsIgnoreCase(normalized)) {
            return "size";
        }
        if ("updateTime".equalsIgnoreCase(normalized)) {
            return "updateTime";
        }
        return "";
    }

    private String normalizeSortOrder(String sortOrder) {
        if (sortOrder == null) {
            return "desc";
        }
        String normalized = sortOrder.trim();
        if ("asc".equalsIgnoreCase(normalized)) {
            return "asc";
        }
        return "desc";
    }

    private void applyFileTypeFilter(LambdaQueryWrapper<FileRecordPO> wrapper, FileSearchQuery query) {
        if (query.getFileType() == null || query.getFileType().isBlank()) {
            return;
        }
        String fileType = query.getFileType().trim().toLowerCase();
        if ("other".equals(fileType)) {
            wrapper.eq(FileRecordPO::getIsDir, false)
                    .and(inner -> inner.notIn(FileRecordPO::getSuffix, List.of(
                                    "png", "jpg", "jpeg", "gif", "webp", "bmp", "svg",
                                    "mp4", "avi", "mov", "mkv", "wmv", "flv", "webm",
                                    "mp3", "wav", "aac", "flac", "ogg", "m4a",
                                    "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
                                    "txt", "md", "csv"))
                            .or()
                            .isNull(FileRecordPO::getSuffix)
                            .or()
                            .eq(FileRecordPO::getSuffix, ""));
        } else if ("image".equals(fileType)) {
            wrapper.eq(FileRecordPO::getIsDir, false)
                    .in(FileRecordPO::getSuffix, List.of("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg"));
        } else if ("video".equals(fileType)) {
            wrapper.eq(FileRecordPO::getIsDir, false)
                    .in(FileRecordPO::getSuffix, List.of("mp4", "avi", "mov", "mkv", "wmv", "flv", "webm"));
        } else if ("audio".equals(fileType)) {
            wrapper.eq(FileRecordPO::getIsDir, false)
                    .in(FileRecordPO::getSuffix, List.of("mp3", "wav", "aac", "flac", "ogg", "m4a"));
        } else if ("document".equals(fileType)) {
            wrapper.eq(FileRecordPO::getIsDir, false)
                    .in(FileRecordPO::getSuffix, List.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "csv"));
        }
    }
}
