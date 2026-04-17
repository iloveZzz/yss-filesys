package com.yss.filesys.application.impl;

import com.yss.filesys.application.command.CreateShareCommand;
import com.yss.filesys.application.command.CreateShareAccessRecordCommand;
import com.yss.filesys.application.dto.FileDownloadDTO;
import com.yss.filesys.application.dto.FileShareDTO;
import com.yss.filesys.application.dto.FileShareAccessRecordDTO;
import com.yss.filesys.application.dto.FileRecordDTO;
import com.yss.filesys.application.dto.FileShareThinDTO;
import com.yss.filesys.application.port.FileShareCommandUseCase;
import com.yss.filesys.application.port.FileShareAccessUseCase;
import com.yss.filesys.application.port.FileShareQueryUseCase;
import com.yss.filesys.common.AnonymousUserContext;
import com.yss.filesys.domain.gateway.FileRecordGateway;
import com.yss.filesys.domain.gateway.FileShareGateway;
import com.yss.filesys.domain.gateway.FileShareAccessRecordGateway;
import com.yss.filesys.domain.gateway.FileShareItemGateway;
import com.yss.filesys.domain.model.BizException;
import com.yss.filesys.domain.model.FileRecord;
import com.yss.filesys.domain.model.FileShareAccessRecord;
import com.yss.filesys.domain.model.FileShareItem;
import com.yss.filesys.domain.model.FileShareRecord;
import com.yss.filesys.storage.plugin.boot.StorageServiceFacade;
import com.yss.filesys.storage.plugin.core.IStorageOperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileShareAppService implements FileShareCommandUseCase, FileShareQueryUseCase, FileShareAccessUseCase {

    private final FileRecordGateway fileRecordGateway;
    private final FileShareGateway fileShareGateway;
    private final FileShareItemGateway fileShareItemGateway;
    private final FileShareAccessRecordGateway fileShareAccessRecordGateway;
    private final StorageServiceFacade storageServiceFacade;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileShareDTO create(CreateShareCommand command) {
        LocalDateTime now = LocalDateTime.now();
        String shareId = UUID.randomUUID().toString().replace("-", "");
        FileShareRecord record = FileShareRecord.builder()
                .shareId(shareId)
                .userId(resolveUserId(command.getUserId()))
                .shareName(command.getShareName() == null || command.getShareName().isBlank() ? "共享文件" : command.getShareName())
                .shareCode(Boolean.TRUE.equals(command.getNeedShareCode()) ? generateCode() : null)
                .expireTime(resolveExpireTime(command))
                .scope(command.getScope() == null || command.getScope().isBlank() ? "preview,download" : command.getScope())
                .viewCount(0)
                .maxViewCount(command.getMaxViewCount())
                .downloadCount(0)
                .maxDownloadCount(command.getMaxDownloadCount())
                .createdAt(now)
                .updatedAt(now)
                .build();
        FileShareRecord saved = fileShareGateway.save(record);
        fileShareItemGateway.saveBatch(command.getFileIds().stream().map(fileId ->
                FileShareItem.builder().shareId(shareId).fileId(fileId).createdAt(now).build()).toList());
        return toDTO(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelByIds(List<String> shareIds) {
        fileShareItemGateway.deleteByShareIds(shareIds);
        fileShareGateway.deleteByIds(shareIds);
    }

    @Override
    public List<FileShareDTO> listByUserId(String userId) {
        userId = resolveUserId(userId);
        return fileShareGateway.listByUserId(userId).stream().map(this::toDTO).toList();
    }

    @Override
    public FileShareDTO getById(String shareId) {
        FileShareRecord record = getShareRecord(shareId);
        return toDTO(record);
    }

    @Override
    public FileShareThinDTO getShareInfo(String shareId) {
        FileShareRecord record = getShareRecord(shareId);
        List<FileShareItem> items = fileShareItemGateway.listByShareId(shareId);
        return FileShareThinDTO.builder()
                .shareId(record.getShareId())
                .shareName(record.getShareName())
                .shareCode(record.getShareCode())
                .fileCount(items.size())
                .hasCheckCode(record.getShareCode() != null && !record.getShareCode().isBlank())
                .isExpire(record.getExpireTime() != null && LocalDateTime.now().isAfter(record.getExpireTime()))
                .expireTime(record.getExpireTime())
                .viewCount(record.getViewCount())
                .downloadCount(record.getDownloadCount())
                .build();
    }

    @Override
    public List<FileRecordDTO> listShareFiles(String shareId, String shareCode) {
        FileShareRecord record = getShareRecord(shareId);
        ensureShareCode(record, shareCode);
        recordAccess(shareId);
        incrementViewCount(record);
        Set<String> fileIds = fileShareItemGateway.listByShareId(shareId).stream()
                .map(FileShareItem::getFileId)
                .collect(Collectors.toSet());
        return fileRecordGateway.listByIds(fileIds).stream()
                .filter(file -> !Boolean.TRUE.equals(file.getIsDeleted()))
                .map(this::toFileDTO)
                .toList();
    }

    @Override
    public boolean verifyShareCode(String shareId, String shareCode) {
        FileShareRecord record = getShareRecord(shareId);
        ensureShareCode(record, shareCode);
        return true;
    }

    @Override
    public FileDownloadDTO downloadShareFile(String shareId, String fileId, String shareCode) {
        FileShareRecord record = getShareRecord(shareId);
        ensureShareCode(record, shareCode);
        if (fileId == null || fileId.isBlank()) {
            throw new BizException("fileId 不能为空");
        }
        boolean inShare = fileShareItemGateway.listByShareId(shareId).stream()
                .anyMatch(item -> fileId.equals(item.getFileId()));
        if (!inShare) {
            throw new BizException("下载失败，该文件不在当前分享内");
        }
        FileRecord fileRecord = fileRecordGateway.findById(fileId).orElseThrow(() -> new BizException("文件不存在: " + fileId));
        if (Boolean.TRUE.equals(fileRecord.getIsDeleted())) {
            throw new BizException("下载失败，该文件已被删除");
        }
        IStorageOperationService storageService = storageServiceFacade.getStorageService(fileRecord.getStorageSettingId());
        if (!storageService.isFileExist(fileRecord.getObjectKey())) {
            throw new BizException("下载失败，该文件不存在");
        }
        recordAccess(shareId);
        incrementDownloadCount(record);
        try {
            return FileDownloadDTO.builder()
                    .fileName(fileRecord.getDisplayName())
                    .fileSize(fileRecord.getSize())
                    .content(storageService.downloadFile(fileRecord.getObjectKey()).readAllBytes())
                    .build();
        } catch (IOException e) {
            throw new BizException("下载失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void record(CreateShareAccessRecordCommand command) {
        if (command.getShareId() == null || command.getShareId().isBlank()) {
            throw new BizException("shareId 不能为空");
        }
        FileShareAccessRecord record = FileShareAccessRecord.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .shareId(command.getShareId())
                .accessIp(command.getAccessIp())
                .accessAddress(command.getAccessAddress())
                .browser(command.getBrowser())
                .os(command.getOs())
                .accessTime(LocalDateTime.now())
                .build();
        fileShareAccessRecordGateway.save(record);
    }

    @Override
    public List<FileShareAccessRecordDTO> listByShareId(String shareId) {
        return fileShareAccessRecordGateway.listByShareId(shareId).stream()
                .map(record -> FileShareAccessRecordDTO.builder()
                        .id(record.getId())
                        .shareId(record.getShareId())
                        .accessIp(record.getAccessIp())
                        .accessAddress(record.getAccessAddress())
                        .browser(record.getBrowser())
                        .os(record.getOs())
                        .accessTime(record.getAccessTime())
                        .build())
                .collect(Collectors.toList());
    }

    private FileShareDTO toDTO(FileShareRecord record) {
        List<String> fileIds = fileShareItemGateway.listByShareId(record.getShareId()).stream().map(FileShareItem::getFileId).toList();
        return FileShareDTO.builder()
                .shareId(record.getShareId())
                .userId(record.getUserId())
                .shareName(record.getShareName())
                .shareCode(record.getShareCode())
                .scope(record.getScope())
                .expireTime(record.getExpireTime())
                .viewCount(record.getViewCount())
                .maxViewCount(record.getMaxViewCount())
                .downloadCount(record.getDownloadCount())
                .maxDownloadCount(record.getMaxDownloadCount())
                .fileIds(fileIds)
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    private String resolveUserId(String userId) {
        return userId == null || userId.isBlank() ? AnonymousUserContext.userId() : userId;
    }

    private FileRecordDTO toFileDTO(FileRecord record) {
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
                .isFavorite(false)
                .uploadTime(record.getUploadTime())
                .updateTime(record.getUpdateTime())
                .build();
    }

    private FileShareRecord getShareRecord(String shareId) {
        FileShareRecord record = fileShareGateway.findById(shareId).orElseThrow(() -> new BizException("分享不存在: " + shareId));
        if (record.getExpireTime() != null && LocalDateTime.now().isAfter(record.getExpireTime())) {
            throw new BizException("分享已过期");
        }
        return record;
    }

    private void ensureShareCode(FileShareRecord record, String shareCode) {
        if (record.getShareCode() == null || record.getShareCode().isBlank()) {
            return;
        }
        if (shareCode == null || shareCode.isBlank() || !record.getShareCode().equals(shareCode)) {
            throw new BizException("提取码不正确");
        }
    }

    private void recordAccess(String shareId) {
        CreateShareAccessRecordCommand command = new CreateShareAccessRecordCommand();
        command.setShareId(shareId);
        record(command);
    }

    private void incrementViewCount(FileShareRecord record) {
        Integer current = record.getViewCount() == null ? 0 : record.getViewCount();
        fileShareGateway.save(record.toBuilder().viewCount(current + 1).updatedAt(LocalDateTime.now()).build());
    }

    private void incrementDownloadCount(FileShareRecord record) {
        Integer current = record.getDownloadCount() == null ? 0 : record.getDownloadCount();
        fileShareGateway.save(record.toBuilder().downloadCount(current + 1).updatedAt(LocalDateTime.now()).build());
    }

    private LocalDateTime resolveExpireTime(CreateShareCommand command) {
        if (command.getExpireType() == null) {
            return null;
        }
        return switch (command.getExpireType()) {
            case 1 -> LocalDateTime.now().plusDays(7);
            case 2 -> LocalDateTime.now().plusDays(30);
            case 3 -> command.getExpireTime();
            case 4 -> null;
            default -> null;
        };
    }

    private String generateCode() {
        int code = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(code);
    }
}
