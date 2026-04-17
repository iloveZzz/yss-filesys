package com.yss.filesys.domain.gateway;

import com.yss.filesys.application.dto.PageDTO;
import com.yss.filesys.application.query.SubscriptionPlanPageQuery;
import com.yss.filesys.domain.model.SubscriptionPlan;

import java.util.Optional;

public interface SubscriptionPlanGateway {

    PageDTO<SubscriptionPlan> page(SubscriptionPlanPageQuery query);

    Optional<SubscriptionPlan> findById(Long id);

    Optional<SubscriptionPlan> findByCode(String planCode);

    Optional<SubscriptionPlan> findByName(String planName);

    Optional<SubscriptionPlan> findDefaultPlan();

    SubscriptionPlan save(SubscriptionPlan plan);

    void deleteById(Long id);
}
