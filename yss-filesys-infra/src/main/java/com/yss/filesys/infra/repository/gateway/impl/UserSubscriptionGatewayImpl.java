package com.yss.filesys.infra.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yss.filesys.domain.gateway.UserSubscriptionGateway;
import com.yss.filesys.domain.model.UserSubscription;
import com.yss.filesys.infra.repository.convertor.UserSubscriptionConvertor;
import com.yss.filesys.infra.repository.entity.UserSubscriptionPO;
import com.yss.filesys.infra.repository.mapper.UserSubscriptionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserSubscriptionGatewayImpl implements UserSubscriptionGateway {

    private final UserSubscriptionMapper userSubscriptionMapper;

    @Override
    public List<UserSubscription> listByPlanId(Long planId) {
        return userSubscriptionMapper.selectList(new LambdaQueryWrapper<UserSubscriptionPO>()
                        .eq(UserSubscriptionPO::getPlanId, planId))
                .stream()
                .map(UserSubscriptionConvertor::toDomain)
                .toList();
    }

    @Override
    public boolean hasActiveByPlanId(Long planId) {
        return userSubscriptionMapper.selectCount(new LambdaQueryWrapper<UserSubscriptionPO>()
                .eq(UserSubscriptionPO::getPlanId, planId)
                .eq(UserSubscriptionPO::getStatus, 0)) > 0;
    }
}
