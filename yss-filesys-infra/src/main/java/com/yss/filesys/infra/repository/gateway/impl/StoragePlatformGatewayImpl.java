package com.yss.filesys.infra.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yss.filesys.domain.gateway.StoragePlatformGateway;
import com.yss.filesys.domain.model.StoragePlatform;
import com.yss.filesys.infra.repository.convertor.StoragePlatformConvertor;
import com.yss.filesys.infra.repository.entity.StoragePlatformPO;
import com.yss.filesys.infra.repository.mapper.StoragePlatformMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StoragePlatformGatewayImpl implements StoragePlatformGateway {

    private final StoragePlatformMapper storagePlatformMapper;

    @Override
    public List<StoragePlatform> listAll() {
        return storagePlatformMapper.selectList(new LambdaQueryWrapper<StoragePlatformPO>()
                        .orderByDesc(StoragePlatformPO::getIsDefault)
                        .orderByAsc(StoragePlatformPO::getId))
                .stream()
                .map(StoragePlatformConvertor::toDomain)
                .toList();
    }

    @Override
    public Optional<StoragePlatform> findByIdentifier(String identifier) {
        StoragePlatformPO po = storagePlatformMapper.selectOne(new LambdaQueryWrapper<StoragePlatformPO>()
                .eq(StoragePlatformPO::getIdentifier, identifier)
                .last("limit 1"));
        return po == null ? Optional.empty() : Optional.of(StoragePlatformConvertor.toDomain(po));
    }

    @Override
    public StoragePlatform save(StoragePlatform platform) {
        StoragePlatformPO po = StoragePlatformConvertor.toPO(platform);
        if (po.getId() == null || storagePlatformMapper.selectById(po.getId()) == null) {
            storagePlatformMapper.insert(po);
        } else {
            storagePlatformMapper.updateById(po);
        }
        return StoragePlatformConvertor.toDomain(po);
    }
}
