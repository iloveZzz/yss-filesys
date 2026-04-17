package com.yss.filesys.infra.repository.convertor;

import com.yss.filesys.domain.model.UserSubscription;
import com.yss.filesys.infra.repository.entity.UserSubscriptionPO;

public final class UserSubscriptionConvertor {

    private UserSubscriptionConvertor() {
    }

    public static UserSubscription toDomain(UserSubscriptionPO po) {
        if (po == null) {
            return null;
        }
        return UserSubscription.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .planId(po.getPlanId())
                .status(po.getStatus())
                .subscriptionDate(po.getSubscriptionDate())
                .expireDate(po.getExpireDate())
                .build();
    }

    public static UserSubscriptionPO toPO(UserSubscription domain) {
        if (domain == null) {
            return null;
        }
        UserSubscriptionPO po = new UserSubscriptionPO();
        po.setId(domain.getId());
        po.setUserId(domain.getUserId());
        po.setPlanId(domain.getPlanId());
        po.setStatus(domain.getStatus());
        po.setSubscriptionDate(domain.getSubscriptionDate());
        po.setExpireDate(domain.getExpireDate());
        return po;
    }
}
