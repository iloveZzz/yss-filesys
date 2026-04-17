package com.yss.filesys.infra.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yss.filesys.domain.gateway.FileShareAccessRecordGateway;
import com.yss.filesys.domain.model.FileShareAccessRecord;
import com.yss.filesys.infra.repository.convertor.FileShareAccessRecordConvertor;
import com.yss.filesys.infra.repository.entity.FileShareAccessRecordPO;
import com.yss.filesys.infra.repository.mapper.FileShareAccessRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class FileShareAccessRecordGatewayImpl implements FileShareAccessRecordGateway {

    private final FileShareAccessRecordMapper fileShareAccessRecordMapper;

    @Override
    public void save(FileShareAccessRecord record) {
        FileShareAccessRecordPO po = FileShareAccessRecordConvertor.toPO(record);
        if (fileShareAccessRecordMapper.selectById(po.getId()) == null) {
            fileShareAccessRecordMapper.insert(po);
        } else {
            fileShareAccessRecordMapper.updateById(po);
        }
    }

    @Override
    public List<FileShareAccessRecord> listByShareId(String shareId) {
        return fileShareAccessRecordMapper.selectList(new LambdaQueryWrapper<FileShareAccessRecordPO>()
                        .eq(FileShareAccessRecordPO::getShareId, shareId)
                        .orderByDesc(FileShareAccessRecordPO::getAccessTime))
                .stream()
                .map(FileShareAccessRecordConvertor::toDomain)
                .toList();
    }
}
