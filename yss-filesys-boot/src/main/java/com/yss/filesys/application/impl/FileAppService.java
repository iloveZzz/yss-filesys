package com.yss.filesys.application.impl;

import com.yss.filesys.application.command.CreateDirectoryCommand;
import com.yss.filesys.application.command.ClearRecycleCommand;
import com.yss.filesys.application.command.FavoriteFilesCommand;
import com.yss.filesys.application.command.MoveFileCommand;
import com.yss.filesys.application.command.MoveToRecycleBinCommand;
import com.yss.filesys.application.command.PermanentlyDeleteRecycleCommand;
import com.yss.filesys.application.command.RenameFileCommand;
import com.yss.filesys.application.command.RestoreRecycleCommand;
import com.yss.filesys.application.dto.FileDownloadDTO;
import com.yss.filesys.application.dto.FileRecordDTO;
import com.yss.filesys.application.dto.PageDTO;
import com.yss.filesys.application.port.FileCommandUseCase;
import com.yss.filesys.application.port.FileFavoriteUseCase;
import com.yss.filesys.application.port.FileQueryUseCase;
import com.yss.filesys.application.port.FileRecycleUseCase;
import com.yss.filesys.application.query.FileSearchQuery;
import com.yss.filesys.domain.gateway.FileRecordGateway;
import com.yss.filesys.domain.gateway.FileShareItemGateway;
import com.yss.filesys.domain.gateway.FileUserFavoriteGateway;
import com.yss.filesys.domain.model.BizException;
import com.yss.filesys.domain.model.FileRecord;
import com.yss.filesys.domain.model.FileUserFavorite;
import com.yss.filesys.storage.plugin.boot.StorageServiceFacade;
import com.yss.filesys.storage.plugin.core.IStorageOperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileAppService implements FileCommandUseCase, FileQueryUseCase, FileRecycleUseCase, FileFavoriteUseCase {

    private final FileRecordGateway fileRecordGateway;
    private final FileShareItemGateway fileShareItemGateway;
    private final FileUserFavoriteGateway fileUserFavoriteGateway;
    private final StorageServiceFacade storageServiceFacade;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileRecordDTO createDirectory(CreateDirectoryCommand command) {
        String folderName = command.getFolderName().trim();
        if (folderName.isEmpty()) {
            throw new BizException("目录名称不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        FileRecord record = FileRecord.builder()
                .fileId(UUID.randomUUID().toString().replace("-", ""))
                .objectKey(null)
                .originalName(folderName)
                .displayName(folderName)
                .isDir(true)
                .parentId(command.getParentId())
                .userId(command.getUserId())
                .storageSettingId(command.getStorageSettingId())
                .uploadTime(now)
                .updateTime(now)
                .isDeleted(false)
                .build();
        return toDTO(fileRecordGateway.save(record), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveToRecycleBin(MoveToRecycleBinCommand command) {
        fileRecordGateway.markDeleted(command.getFileIds(), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renameFile(String fileId, RenameFileCommand command) {
        validateUser(command.getUserId());
        FileRecord record = loadOwnedFile(fileId, command.getUserId());
        String fileName = command.getFileName() == null ? "" : command.getFileName().trim();
        if (fileName.isEmpty()) {
            throw new BizException("文件名称不能为空");
        }
        fileRecordGateway.save(record.toBuilder()
                .originalName(fileName)
                .displayName(fileName)
                .updateTime(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveFile(MoveFileCommand command) {
        validateUser(command.getUserId());
        FileRecord record = loadOwnedFile(command.getFileId(), command.getUserId());
        if (command.getTargetParentId() != null && !command.getTargetParentId().isBlank()) {
            FileRecord parent = fileRecordGateway.findById(command.getTargetParentId())
                    .orElseThrow(() -> new BizException("目标目录不存在: " + command.getTargetParentId()));
            if (!command.getUserId().equals(parent.getUserId()) || !Boolean.TRUE.equals(parent.getIsDir())) {
                throw new BizException("目标目录无效");
            }
        }
        fileRecordGateway.save(record.toBuilder()
                .parentId(command.getTargetParentId())
                .updateTime(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(RestoreRecycleCommand command) {
        validateUser(command.getUserId());
        Set<String> restoreIds = collectRecursiveIds(command.getUserId(), command.getFileIds(), true);
        if (restoreIds.isEmpty()) {
            throw new BizException("未找到要恢复的文件");
        }
        fileRecordGateway.markDeleted(restoreIds.stream().toList(), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void permanentlyDelete(PermanentlyDeleteRecycleCommand command) {
        validateUser(command.getUserId());
        Set<String> deleteIds = collectRecursiveIds(command.getUserId(), command.getFileIds(), true);
        if (deleteIds.isEmpty()) {
            throw new BizException("未找到要删除的文件");
        }
        deleteRecycleRecords(deleteIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearRecycle(ClearRecycleCommand command) {
        validateUser(command.getUserId());
        List<FileRecord> deletedRecords = fileRecordGateway.listDeletedByUser(command.getUserId());
        if (deletedRecords.isEmpty()) {
            return;
        }
        deleteRecycleRecords(deletedRecords.stream().map(FileRecord::getFileId).collect(Collectors.toSet()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int purgeExpiredRecycleItems() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        List<FileRecord> expired = fileRecordGateway.listDeletedBefore(cutoff);
        if (expired.isEmpty()) {
            return 0;
        }
        deleteRecycleRecords(expired.stream().map(FileRecord::getFileId).collect(Collectors.toSet()));
        return expired.size();
    }

    @Override
    public PageDTO<FileRecordDTO> search(FileSearchQuery query) {
        if (query.getUserId() == null || query.getUserId().isBlank()) {
            throw new BizException("userId 不能为空");
        }
        if (Boolean.TRUE.equals(query.getFavorite())) {
            List<String> favoriteIds = fileUserFavoriteGateway.listFileIdsByUserId(query.getUserId());
            if (favoriteIds.isEmpty()) {
                return PageDTO.<FileRecordDTO>builder()
                        .total(0L)
                        .pageNo(query.getPageNo())
                        .pageSize(query.getPageSize())
                        .records(List.of())
                        .build();
            }
            query.setFileIds(favoriteIds);
        }
        List<FileRecord> records = fileRecordGateway.search(query);
        Set<String> favoriteIds = fileUserFavoriteGateway.findFileIdsByUserId(
                query.getUserId(),
                records.stream().map(FileRecord::getFileId).collect(Collectors.toSet())
        );
        return PageDTO.<FileRecordDTO>builder()
                .total(fileRecordGateway.count(query))
                .pageNo(query.getPageNo())
                .pageSize(query.getPageSize())
                .records(records.stream().map(record -> toDTO(record, favoriteIds.contains(record.getFileId()))).toList())
                .build();
    }

    @Override
    public FileRecordDTO getById(String fileId) {
        FileRecord record = fileRecordGateway.findById(fileId).orElseThrow(() -> new BizException("文件不存在: " + fileId));
        return toDTO(record, false);
    }

    @Override
    public List<FileRecordDTO> listDirs(String userId, String parentId) {
        validateUser(userId);
        return fileRecordGateway.listByUserAndParentAndDeleted(userId, parentId, false).stream()
                .filter(record -> Boolean.TRUE.equals(record.getIsDir()))
                .map(record -> toDTO(record, false))
                .toList();
    }

    @Override
    public List<FileRecordDTO> getDirectoryTreePath(String userId, String dirId) {
        validateUser(userId);
        if (dirId == null || dirId.isBlank()) {
            return List.of();
        }
        java.util.LinkedList<FileRecord> path = new java.util.LinkedList<>();
        FileRecord current = fileRecordGateway.findById(dirId).orElseThrow(() -> new BizException("目录不存在: " + dirId));
        if (!userId.equals(current.getUserId())) {
            throw new BizException("无权访问该目录");
        }
        while (current != null) {
            path.addFirst(current);
            if (current.getParentId() == null || current.getParentId().isBlank()) {
                break;
            }
            current = fileRecordGateway.findById(current.getParentId()).orElse(null);
        }
        return path.stream().map(record -> toDTO(record, false)).toList();
    }

    @Override
    public String getFileUrl(String fileId, String userId, Integer expireSeconds) {
        FileRecord record = loadOwnedFile(fileId, userId);
        if (Boolean.TRUE.equals(record.getIsDir())) {
            throw new BizException("目录不支持下载");
        }
        if (record.getObjectKey() == null || record.getObjectKey().isBlank()) {
            throw new BizException("文件不存在或未上传完成");
        }
        StringBuilder url = new StringBuilder("/api/files/download/").append(fileId).append("?userId=").append(userId);
        if (expireSeconds != null && expireSeconds > 0) {
            url.append("&expireSeconds=").append(expireSeconds);
        }
        return url.toString();
    }

    @Override
    public FileDownloadDTO downloadFile(String fileId, String userId) {
        FileRecord record = loadOwnedFile(fileId, userId);
        if (Boolean.TRUE.equals(record.getIsDir())) {
            throw new BizException("目录不支持下载");
        }
        IStorageOperationService storageService = storageServiceFacade.getStorageService(record.getStorageSettingId());
        if (!storageService.isFileExist(record.getObjectKey())) {
            throw new BizException("文件不存在");
        }
        try {
            return FileDownloadDTO.builder()
                    .fileName(record.getDisplayName())
                    .fileSize(record.getSize())
                    .content(storageService.downloadFile(record.getObjectKey()).readAllBytes())
                    .build();
        } catch (IOException e) {
            throw new BizException("下载失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favorite(FavoriteFilesCommand command) {
        validateUser(command.getUserId());
        Set<String> favoriteIds = collectFavoriteCandidates(command.getUserId(), command.getFileIds());
        if (favoriteIds.isEmpty()) {
            throw new BizException("没有找到可收藏的文件");
        }
        LocalDateTime now = LocalDateTime.now();
        List<FileUserFavorite> favorites = favoriteIds.stream()
                .map(fileId -> FileUserFavorite.builder()
                        .userId(command.getUserId())
                        .fileId(fileId)
                        .createdAt(now)
                        .build())
                .toList();
        fileUserFavoriteGateway.saveBatch(favorites);
        touchFiles(favoriteIds, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfavorite(FavoriteFilesCommand command) {
        validateUser(command.getUserId());
        Set<String> fileIds = normalizeFileIds(command.getFileIds());
        if (fileIds.isEmpty()) {
            return;
        }
        fileUserFavoriteGateway.deleteByUserAndFileIds(command.getUserId(), fileIds);
        touchFiles(fileIds, LocalDateTime.now());
    }

    @Override
    public long count(String userId) {
        validateUser(userId);
        List<String> favoriteIds = fileUserFavoriteGateway.listFileIdsByUserId(userId);
        if (favoriteIds.isEmpty()) {
            return 0L;
        }
        return fileRecordGateway.listByIds(favoriteIds).stream()
                .filter(record -> !Boolean.TRUE.equals(record.getIsDeleted()))
                .count();
    }

    private FileRecordDTO toDTO(FileRecord record, boolean favorite) {
        return FileRecordDTO.builder()
                .fileId(record.getFileId())
                .originalName(record.getOriginalName())
                .displayName(record.getDisplayName())
                .suffix(record.getSuffix())
                .size(record.getSize())
                .isDir(record.getIsDir())
                .parentId(record.getParentId())
                .userId(record.getUserId())
                .storageSettingId(record.getStorageSettingId())
                .isDeleted(record.getIsDeleted())
                .isFavorite(favorite)
                .uploadTime(record.getUploadTime())
                .updateTime(record.getUpdateTime())
                .build();
    }

    private void validateUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new BizException("userId 不能为空");
        }
    }

    private FileRecord loadOwnedFile(String fileId, String userId) {
        FileRecord record = fileRecordGateway.findById(fileId).orElseThrow(() -> new BizException("文件不存在: " + fileId));
        if (!userId.equals(record.getUserId())) {
            throw new BizException("无权操作该文件");
        }
        return record;
    }

    private Set<String> collectRecursiveIds(String userId, Collection<String> rootIds, boolean deletedOnly) {
        if (rootIds == null || rootIds.isEmpty()) {
            return Set.of();
        }
        Set<String> collected = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>(rootIds);
        while (!queue.isEmpty()) {
            String currentId = queue.removeFirst();
            if (!collected.add(currentId)) {
                continue;
            }
            FileRecord current = fileRecordGateway.findById(currentId).orElse(null);
            if (current == null || !userId.equals(current.getUserId())) {
                continue;
            }
            if (deletedOnly && !Boolean.TRUE.equals(current.getIsDeleted())) {
                continue;
            }
            List<FileRecord> children = fileRecordGateway.listByUserAndParentAndDeleted(
                    userId,
                    currentId,
                    true
            );
            for (FileRecord child : children) {
                queue.addLast(child.getFileId());
            }
        }
        return collected;
    }

    private Set<String> collectFavoriteCandidates(String userId, Collection<String> fileIds) {
        Set<String> normalized = normalizeFileIds(fileIds);
        if (normalized.isEmpty()) {
            return Set.of();
        }
        return fileRecordGateway.listByIds(normalized).stream()
                .filter(record -> userId.equals(record.getUserId()))
                .filter(record -> !Boolean.TRUE.equals(record.getIsDeleted()))
                .map(FileRecord::getFileId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Set<String> normalizeFileIds(Collection<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Set.of();
        }
        return fileIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private void touchFiles(Collection<String> fileIds, LocalDateTime time) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        for (String fileId : fileIds) {
            FileRecord record = fileRecordGateway.findById(fileId).orElse(null);
            if (record == null) {
                continue;
            }
            fileRecordGateway.save(record.toBuilder().lastAccessTime(time).updateTime(time).build());
        }
    }

    private void deleteRecycleRecords(Set<String> fileIds) {
        List<FileRecord> records = fileRecordGateway.listByIds(fileIds);
        if (records.isEmpty()) {
            return;
        }

        java.util.Map<String, String> deletions = new java.util.LinkedHashMap<>();
        for (FileRecord record : records) {
            String objectKey = record.getObjectKey();
            if (objectKey == null || objectKey.isBlank()) {
                continue;
            }
            if (fileRecordGateway.countByObjectKeyExcludingIds(objectKey, fileIds) == 0) {
                deletions.putIfAbsent(objectKey, record.getStorageSettingId());
            }
        }

        fileRecordGateway.deleteByIds(fileIds);
        fileShareItemGateway.deleteByFileIds(fileIds);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (java.util.Map.Entry<String, String> entry : deletions.entrySet()) {
                    try {
                        storageServiceFacade.getStorageService(entry.getValue()).deleteFile(entry.getKey());
                    } catch (Exception e) {
                        // 物理删除失败不回滚数据库事务，保留告警即可
                    }
                }
            }
        });
    }
}
