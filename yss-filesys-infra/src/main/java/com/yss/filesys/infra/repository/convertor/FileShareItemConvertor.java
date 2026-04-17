package com.yss.filesys.infra.repository.convertor;

import com.yss.filesys.domain.model.FileShareItem;
import com.yss.filesys.infra.repository.entity.FileShareItemPO;

public final class FileShareItemConvertor {

    private FileShareItemConvertor() {
    }

    public static FileShareItem toDomain(FileShareItemPO po) {
        return FileShareItem.builder()
                .shareId(po.getShareId())
                .fileId(po.getFileId())
                .createdAt(po.getCreatedAt())
                .build();
    }

    public static FileShareItemPO toPO(FileShareItem domain) {
        FileShareItemPO po = new FileShareItemPO();
        po.setShareId(domain.getShareId());
        po.setFileId(domain.getFileId());
        po.setCreatedAt(domain.getCreatedAt());
        return po;
    }
}
