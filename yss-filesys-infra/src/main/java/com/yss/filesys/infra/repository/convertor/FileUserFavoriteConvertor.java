package com.yss.filesys.infra.repository.convertor;

import com.yss.filesys.domain.model.FileUserFavorite;
import com.yss.filesys.infra.repository.entity.FileUserFavoritePO;

public final class FileUserFavoriteConvertor {

    private FileUserFavoriteConvertor() {
    }

    public static FileUserFavorite toDomain(FileUserFavoritePO po) {
        return FileUserFavorite.builder()
                .userId(po.getUserId())
                .fileId(po.getFileId())
                .createdAt(po.getCreatedAt())
                .build();
    }

    public static FileUserFavoritePO toPO(FileUserFavorite domain) {
        FileUserFavoritePO po = new FileUserFavoritePO();
        po.setUserId(domain.getUserId());
        po.setFileId(domain.getFileId());
        po.setCreatedAt(domain.getCreatedAt());
        return po;
    }
}
