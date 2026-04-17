package com.yss.filesys.domain.gateway;

import com.yss.filesys.domain.model.FileUserFavorite;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface FileUserFavoriteGateway {

    void saveBatch(List<FileUserFavorite> favorites);

    void deleteByUserAndFileIds(String userId, Collection<String> fileIds);

    long countByUserId(String userId);

    Set<String> findFileIdsByUserId(String userId, Collection<String> fileIds);

    List<String> listFileIdsByUserId(String userId);
}
