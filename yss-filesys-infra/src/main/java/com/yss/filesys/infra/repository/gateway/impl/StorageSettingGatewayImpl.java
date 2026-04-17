package com.yss.filesys.infra.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yss.filesys.domain.gateway.StorageSettingGateway;
import com.yss.filesys.domain.model.StorageSetting;
import com.yss.filesys.infra.repository.convertor.StorageSettingConvertor;
import com.yss.filesys.infra.repository.entity.StorageSettingPO;
import com.yss.filesys.infra.repository.mapper.StorageSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StorageSettingGatewayImpl implements StorageSettingGateway {

    private final StorageSettingMapper storageSettingMapper;

    @Override
    public List<StorageSetting> listByUserId(String userId) {
        return storageSettingMapper.selectList(
                        new LambdaQueryWrapper<StorageSettingPO>()
                                .eq(StorageSettingPO::getUserId, userId)
                                .orderByDesc(StorageSettingPO::getUpdatedAt))
                .stream()
                .map(StorageSettingConvertor::toDomain)
                .toList();
    }

    @Override
    public Optional<StorageSetting> findById(String id) {
        StorageSettingPO po = storageSettingMapper.selectById(id);
        return po == null ? Optional.empty() : Optional.of(StorageSettingConvertor.toDomain(po));
    }

    @Override
    public StorageSetting save(StorageSetting setting) {
        StorageSettingPO po = StorageSettingConvertor.toPO(setting);
        if (storageSettingMapper.selectById(po.getId()) == null) {
            storageSettingMapper.insert(po);
        } else {
            storageSettingMapper.updateById(po);
        }
        return StorageSettingConvertor.toDomain(po);
    }

    @Override
    public void updateEnabled(String id, Integer enabled) {
        storageSettingMapper.update(
                null,
                new LambdaUpdateWrapper<StorageSettingPO>()
                        .eq(StorageSettingPO::getId, id)
                        .set(StorageSettingPO::getEnabled, enabled)
                        .set(StorageSettingPO::getUpdatedAt, LocalDateTime.now())
        );
    }
}
