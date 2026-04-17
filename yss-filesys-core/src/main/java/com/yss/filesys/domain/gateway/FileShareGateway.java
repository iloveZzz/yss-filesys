package com.yss.filesys.domain.gateway;

import com.yss.filesys.domain.model.FileShareRecord;

import java.util.List;
import java.util.Optional;

public interface FileShareGateway {

    FileShareRecord save(FileShareRecord shareRecord);

    Optional<FileShareRecord> findById(String shareId);

    List<FileShareRecord> listByUserId(String userId);

    void deleteByIds(List<String> shareIds);
}
