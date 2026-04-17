package com.yss.filesys.infra.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yss.filesys.domain.gateway.FileUserFavoriteGateway;
import com.yss.filesys.domain.model.FileUserFavorite;
import com.yss.filesys.infra.repository.convertor.FileUserFavoriteConvertor;
import com.yss.filesys.infra.repository.entity.FileUserFavoritePO;
import com.yss.filesys.infra.repository.mapper.FileUserFavoriteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class FileUserFavoriteGatewayImpl implements FileUserFavoriteGateway {

    private final FileUserFavoriteMapper fileUserFavoriteMapper;

    @Override
    public void saveBatch(List<FileUserFavorite> favorites) {
        if (favorites == null || favorites.isEmpty()) {
            return;
        }
        for (FileUserFavorite favorite : favorites) {
            FileUserFavoritePO po = FileUserFavoriteConvertor.toPO(favorite);
            long exists = fileUserFavoriteMapper.selectCount(new LambdaQueryWrapper<FileUserFavoritePO>()
                    .eq(FileUserFavoritePO::getUserId, po.getUserId())
                    .eq(FileUserFavoritePO::getFileId, po.getFileId()));
            if (exists == 0) {
                fileUserFavoriteMapper.insert(po);
            }
        }
    }

    @Override
    public void deleteByUserAndFileIds(String userId, Collection<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        fileUserFavoriteMapper.delete(new LambdaQueryWrapper<FileUserFavoritePO>()
                .eq(FileUserFavoritePO::getUserId, userId)
                .in(FileUserFavoritePO::getFileId, fileIds));
    }

    @Override
    public long countByUserId(String userId) {
        return fileUserFavoriteMapper.selectCount(new LambdaQueryWrapper<FileUserFavoritePO>()
                .eq(FileUserFavoritePO::getUserId, userId));
    }

    @Override
    public Set<String> findFileIdsByUserId(String userId, Collection<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Set.of();
        }
        List<FileUserFavoritePO> list = fileUserFavoriteMapper.selectList(new LambdaQueryWrapper<FileUserFavoritePO>()
                .eq(FileUserFavoritePO::getUserId, userId)
                .in(FileUserFavoritePO::getFileId, fileIds));
        Set<String> result = new HashSet<>();
        for (FileUserFavoritePO po : list) {
            result.add(po.getFileId());
        }
        return result;
    }

    @Override
    public List<String> listFileIdsByUserId(String userId) {
        return fileUserFavoriteMapper.selectList(new LambdaQueryWrapper<FileUserFavoritePO>()
                        .eq(FileUserFavoritePO::getUserId, userId))
                .stream()
                .map(FileUserFavoritePO::getFileId)
                .toList();
    }
}
