package com.yss.filesys.infra.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.filesys.application.dto.PageDTO;
import com.yss.filesys.application.query.SubscriptionPlanPageQuery;
import com.yss.filesys.domain.gateway.SubscriptionPlanGateway;
import com.yss.filesys.domain.model.SubscriptionPlan;
import com.yss.filesys.infra.repository.convertor.SubscriptionPlanConvertor;
import com.yss.filesys.infra.repository.entity.SubscriptionPlanPO;
import com.yss.filesys.infra.repository.mapper.SubscriptionPlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SubscriptionPlanGatewayImpl implements SubscriptionPlanGateway {

    private final SubscriptionPlanMapper subscriptionPlanMapper;

    @Override
    public PageDTO<SubscriptionPlan> page(SubscriptionPlanPageQuery query) {
        Page<SubscriptionPlanPO> page = subscriptionPlanMapper.selectPage(
                new Page<>(query.getPageNo(), query.getPageSize()),
                buildPageWrapper(query)
        );
        return PageDTO.<SubscriptionPlan>builder()
                .total(page.getTotal())
                .pageIndex(page.getCurrent())
                .pageSize(page.getSize())
                .records(page.getRecords().stream().map(SubscriptionPlanConvertor::toDomain).toList())
                .build();
    }

    @Override
    public Optional<SubscriptionPlan> findById(Long id) {
        SubscriptionPlanPO po = subscriptionPlanMapper.selectById(id);
        return Optional.ofNullable(SubscriptionPlanConvertor.toDomain(po));
    }

    @Override
    public Optional<SubscriptionPlan> findByCode(String planCode) {
        return Optional.ofNullable(subscriptionPlanMapper.selectOne(new LambdaQueryWrapper<SubscriptionPlanPO>()
                .eq(SubscriptionPlanPO::getPlanCode, planCode)
                .last("limit 1"))).map(SubscriptionPlanConvertor::toDomain);
    }

    @Override
    public Optional<SubscriptionPlan> findByName(String planName) {
        return Optional.ofNullable(subscriptionPlanMapper.selectOne(new LambdaQueryWrapper<SubscriptionPlanPO>()
                .eq(SubscriptionPlanPO::getPlanName, planName)
                .last("limit 1"))).map(SubscriptionPlanConvertor::toDomain);
    }

    @Override
    public Optional<SubscriptionPlan> findDefaultPlan() {
        return Optional.ofNullable(subscriptionPlanMapper.selectOne(new LambdaQueryWrapper<SubscriptionPlanPO>()
                .eq(SubscriptionPlanPO::getIsDefault, 1)
                .orderByAsc(SubscriptionPlanPO::getSortOrder)
                .last("limit 1"))).map(SubscriptionPlanConvertor::toDomain);
    }

    @Override
    public SubscriptionPlan save(SubscriptionPlan plan) {
        SubscriptionPlanPO po = SubscriptionPlanConvertor.toPO(plan);
        if (po.getId() == null) {
            subscriptionPlanMapper.insert(po);
        } else {
            subscriptionPlanMapper.updateById(po);
        }
        return SubscriptionPlanConvertor.toDomain(po);
    }

    @Override
    public void deleteById(Long id) {
        subscriptionPlanMapper.deleteById(id);
    }

    private LambdaQueryWrapper<SubscriptionPlanPO> buildPageWrapper(SubscriptionPlanPageQuery query) {
        LambdaQueryWrapper<SubscriptionPlanPO> wrapper = new LambdaQueryWrapper<SubscriptionPlanPO>()
                .orderByAsc(SubscriptionPlanPO::getSortOrder);
        if (StringUtils.hasText(query.getPlanName())) {
            wrapper.like(SubscriptionPlanPO::getPlanName, query.getPlanName());
        }
        return wrapper;
    }
}
