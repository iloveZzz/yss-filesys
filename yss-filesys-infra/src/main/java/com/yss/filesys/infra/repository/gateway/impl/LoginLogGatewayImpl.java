package com.yss.filesys.infra.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.filesys.application.dto.PageDTO;
import com.yss.filesys.domain.gateway.LoginLogGateway;
import com.yss.filesys.domain.model.LoginLogRecord;
import com.yss.filesys.infra.repository.convertor.LoginLogConvertor;
import com.yss.filesys.infra.repository.entity.LoginLogPO;
import com.yss.filesys.infra.repository.mapper.LoginLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class LoginLogGatewayImpl implements LoginLogGateway {

    private final LoginLogMapper loginLogMapper;

    @Override
    public void save(LoginLogRecord record) {
        LoginLogPO po = LoginLogConvertor.toPO(record);
        if (po.getId() == null) {
            loginLogMapper.insert(po);
        } else {
            loginLogMapper.updateById(po);
        }
    }

    @Override
    public PageDTO<LoginLogRecord> page(String userId, String keyword, Integer status, long pageNo, long pageSize) {
        LambdaQueryWrapper<LoginLogPO> wrapper = new LambdaQueryWrapper<LoginLogPO>()
                .eq(StringUtils.hasText(userId), LoginLogPO::getUserId, userId)
                .and(StringUtils.hasText(keyword), w -> w.like(LoginLogPO::getUsername, keyword)
                        .or().like(LoginLogPO::getLoginIp, keyword)
                        .or().like(LoginLogPO::getLoginAddress, keyword)
                        .or().like(LoginLogPO::getBrowser, keyword)
                        .or().like(LoginLogPO::getOs, keyword)
                        .or().like(LoginLogPO::getMsg, keyword))
                .eq(status != null, LoginLogPO::getStatus, status)
                .orderByDesc(LoginLogPO::getLoginTime);

        IPage<LoginLogPO> page = loginLogMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<LoginLogRecord> records = page.getRecords().stream().map(LoginLogConvertor::toDomain).toList();
        return PageDTO.<LoginLogRecord>builder()
                .total(page.getTotal())
                .pageNo(page.getCurrent())
                .pageSize(page.getSize())
                .records(records)
                .build();
    }
}
