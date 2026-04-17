package com.yss.filesys.infra.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yss.filesys.domain.gateway.FileShareItemGateway;
import com.yss.filesys.domain.model.FileShareItem;
import com.yss.filesys.infra.repository.convertor.FileShareItemConvertor;
import com.yss.filesys.infra.repository.entity.FileShareItemPO;
import com.yss.filesys.infra.repository.mapper.FileShareItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class FileShareItemGatewayImpl implements FileShareItemGateway {

    private final FileShareItemMapper fileShareItemMapper;

    @Override
    public void saveBatch(List<FileShareItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        items.stream()
                .map(FileShareItemConvertor::toPO)
                .forEach(fileShareItemMapper::insert);
    }

    @Override
    public List<FileShareItem> listByShareId(String shareId) {
        return fileShareItemMapper.selectList(new LambdaQueryWrapper<FileShareItemPO>()
                        .eq(FileShareItemPO::getShareId, shareId))
                .stream()
                .map(FileShareItemConvertor::toDomain)
                .toList();
    }

    @Override
    public void deleteByShareIds(List<String> shareIds) {
        if (shareIds == null || shareIds.isEmpty()) {
            return;
        }
        fileShareItemMapper.delete(new LambdaQueryWrapper<FileShareItemPO>()
                .in(FileShareItemPO::getShareId, shareIds));
    }

    @Override
    public void deleteByFileIds(Collection<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        fileShareItemMapper.delete(new LambdaQueryWrapper<FileShareItemPO>()
                .in(FileShareItemPO::getFileId, fileIds));
    }
}
