package com.yss.filesys.infra.repository.convertor;

import com.yss.filesys.domain.model.FileShareRecord;
import com.yss.filesys.infra.repository.entity.FileSharePO;

public final class FileShareConvertor {

    private FileShareConvertor() {
    }

    public static FileShareRecord toDomain(FileSharePO po) {
        return FileShareRecord.builder()
                .shareId(po.getId())
                .userId(po.getUserId())
                .shareName(po.getShareName())
                .shareCode(po.getShareCode())
                .expireTime(po.getExpireTime())
                .scope(po.getScope())
                .viewCount(po.getViewCount())
                .maxViewCount(po.getMaxViewCount())
                .downloadCount(po.getDownloadCount())
                .maxDownloadCount(po.getMaxDownloadCount())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    public static FileSharePO toPO(FileShareRecord domain) {
        FileSharePO po = new FileSharePO();
        po.setId(domain.getShareId());
        po.setUserId(domain.getUserId());
        po.setShareName(domain.getShareName());
        po.setShareCode(domain.getShareCode());
        po.setExpireTime(domain.getExpireTime());
        po.setScope(domain.getScope());
        po.setViewCount(domain.getViewCount());
        po.setMaxViewCount(domain.getMaxViewCount());
        po.setDownloadCount(domain.getDownloadCount());
        po.setMaxDownloadCount(domain.getMaxDownloadCount());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }
}
