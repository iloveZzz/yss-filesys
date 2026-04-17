package com.yss.filesys.application.impl;

import com.yss.filesys.application.dto.FileHomeDTO;
import com.yss.filesys.application.dto.FileHomeUsedBytesDTO;
import com.yss.filesys.application.port.FileHomeUseCase;
import com.yss.filesys.common.AnonymousUserContext;
import com.yss.filesys.domain.gateway.FileRecordGateway;
import com.yss.filesys.domain.gateway.FileUserFavoriteGateway;
import com.yss.filesys.domain.model.FileRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileHomeAppService implements FileHomeUseCase {

    private final FileRecordGateway fileRecordGateway;
    private final FileUserFavoriteGateway fileUserFavoriteGateway;

    @Override
    public FileHomeDTO getHome(String userId, Integer unit, Integer dateType) {
        userId = resolveUserId(userId);
        long fileCount = fileRecordGateway.countByUserAndDeleted(userId, false);
        long directoryCount = fileRecordGateway.listByUserAndDeleted(userId, false).stream()
                .filter(record -> Boolean.TRUE.equals(record.getIsDir()))
                .count();
        long recycleCount = fileRecordGateway.countByUserAndDeleted(userId, true);
        long favoriteCount = fileUserFavoriteGateway.listFileIdsByUserId(userId).size();
        List<FileRecord> records = fileRecordGateway.listByUserAndDeleted(userId, false);
        long totalBytes = records.stream()
                .filter(record -> !Boolean.TRUE.equals(record.getIsDir()))
                .mapToLong(record -> record.getSize() == null ? 0L : record.getSize())
                .sum();
        List<FileHomeUsedBytesDTO> usedBytes = buildUsedBytes(records, unit, dateType);
        List<com.yss.filesys.application.dto.FileRecordDTO> recentFiles = records.stream()
                .filter(record -> !Boolean.TRUE.equals(record.getIsDir()))
                .limit(20)
                .map(record -> com.yss.filesys.application.dto.FileRecordDTO.builder()
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
                        .build())
                .collect(Collectors.toList());
        return FileHomeDTO.builder()
                .fileCount(fileCount)
                .directoryCount(directoryCount)
                .recycleCount(recycleCount)
                .favoriteCount(favoriteCount)
                .totalBytes(totalBytes)
                .unit(unit)
                .dateType(dateType)
                .usedBytes(usedBytes)
                .recentFiles(recentFiles)
                .build();
    }

    private String resolveUserId(String userId) {
        return userId == null || userId.isBlank() ? AnonymousUserContext.userId() : userId;
    }

    private List<FileHomeUsedBytesDTO> buildUsedBytes(List<FileRecord> records, Integer unit, Integer dateType) {
        int normalizedUnit = unit == null || unit <= 0 ? 2 : unit;
        int normalizedDateType = dateType == null || dateType < 0 || dateType > 2 ? 1 : dateType;
        LocalDate end = LocalDate.now();
        LocalDate start = switch (normalizedDateType) {
            case 0 -> end.minusMonths(3);
            case 2 -> end.minusDays(7);
            default -> end.minusDays(30);
        };
        Map<LocalDate, Long> data = new LinkedHashMap<>();
        for (FileRecord record : records) {
            if (Boolean.TRUE.equals(record.getIsDir())) {
                continue;
            }
            LocalDateTime time = record.getUploadTime();
            if (time == null) {
                continue;
            }
            LocalDate date = time.toLocalDate();
            if (date.isBefore(start) || date.isAfter(end)) {
                continue;
            }
            long size = record.getSize() == null ? 0L : record.getSize();
            data.merge(date, size, Long::sum);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return data.entrySet().stream()
                .map(entry -> FileHomeUsedBytesDTO.builder()
                        .label(entry.getKey().format(formatter))
                        .usedBytes(convertUnit(entry.getValue(), normalizedUnit))
                        .build())
                .toList();
    }

    private long convertUnit(long bytes, int unit) {
        return switch (unit) {
            case 1 -> bytes / 1024L;
            case 3 -> bytes / 1024L / 1024L / 1024L;
            default -> bytes / 1024L / 1024L;
        };
    }
}
