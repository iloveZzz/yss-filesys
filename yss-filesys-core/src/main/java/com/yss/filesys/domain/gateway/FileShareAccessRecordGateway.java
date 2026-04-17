package com.yss.filesys.domain.gateway;

import com.yss.filesys.domain.model.FileShareAccessRecord;

import java.util.List;

public interface FileShareAccessRecordGateway {

    void save(FileShareAccessRecord record);

    List<FileShareAccessRecord> listByShareId(String shareId);
}
