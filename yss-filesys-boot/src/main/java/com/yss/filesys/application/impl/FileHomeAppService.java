package com.yss.filesys.application.impl;

import com.yss.filesys.application.dto.FileHomeDTO;
import com.yss.filesys.application.dto.FileHomeUsedBytesDTO;
import com.yss.filesys.application.port.FileHomeUseCase;
import com.yss.filesys.domain.gateway.FileRecordGateway;
import com.yss.filesys.domain.gateway.FileUserFavoriteGateway;
import com.yss.filesys.domain.model.BizException;
import com.yss.filesys.domain.model.FileRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileHomeAppService implements FileHomeUseCase {

    private final FileRecordGateway fileRecordGateway;
    private final FileUserFavoriteGateway fileUserFavoriteGateway;

    @Override
    public FileHomeDTO getHome(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new BizException("userId 不能为空");
        }
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
        List<FileHomeUsedBytesDTO> usedBytes = buildUsedBytes(records);
        return FileHomeDTO.builder()
                .fileCount(fileCount)
                .directoryCount(directoryCount)
                .recycleCount(recycleCount)
                .favoriteCount(favoriteCount)
                .totalBytes(totalBytes)
                .usedBytes(usedBytes)
                .build();
    }

    private List<FileHomeUsedBytesDTO> buildUsedBytes(List<FileRecord> records) {
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
            long size = record.getSize() == null ? 0L : record.getSize();
            data.merge(date, size, Long::sum);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return data.entrySet().stream()
                .map(entry -> FileHomeUsedBytesDTO.builder()
                        .label(entry.getKey().format(formatter))
                        .usedBytes(entry.getValue())
                        .build())
                .toList();
    }
}
