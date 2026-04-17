package com.yss.filesys.infra.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yss.filesys.domain.gateway.FileShareGateway;
import com.yss.filesys.domain.model.FileShareRecord;
import com.yss.filesys.infra.repository.convertor.FileShareConvertor;
import com.yss.filesys.infra.repository.entity.FileSharePO;
import com.yss.filesys.infra.repository.mapper.FileShareMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FileShareGatewayImpl implements FileShareGateway {

    private final FileShareMapper fileShareMapper;

    @Override
    public FileShareRecord save(FileShareRecord shareRecord) {
        FileSharePO po = FileShareConvertor.toPO(shareRecord);
        if (fileShareMapper.selectById(po.getId()) == null) {
            fileShareMapper.insert(po);
        } else {
            fileShareMapper.updateById(po);
        }
        return FileShareConvertor.toDomain(po);
    }

    @Override
    public Optional<FileShareRecord> findById(String shareId) {
        FileSharePO po = fileShareMapper.selectById(shareId);
        return po == null ? Optional.empty() : Optional.of(FileShareConvertor.toDomain(po));
    }

    @Override
    public List<FileShareRecord> listByUserId(String userId) {
        return fileShareMapper.selectList(new LambdaQueryWrapper<FileSharePO>()
                        .eq(FileSharePO::getUserId, userId)
                        .orderByDesc(FileSharePO::getCreatedAt))
                .stream().map(FileShareConvertor::toDomain).toList();
    }

    @Override
    public void deleteByIds(List<String> shareIds) {
        if (shareIds == null || shareIds.isEmpty()) {
            return;
        }
        fileShareMapper.deleteByIds(shareIds);
    }
}
