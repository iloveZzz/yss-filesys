package com.yss.filesys.domain.gateway;

import com.yss.filesys.domain.model.UserSubscription;

import java.util.List;

public interface UserSubscriptionGateway {

    List<UserSubscription> listByPlanId(Long planId);

    boolean hasActiveByPlanId(Long planId);
}
