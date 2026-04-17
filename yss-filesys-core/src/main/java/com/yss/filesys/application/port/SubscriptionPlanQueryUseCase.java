package com.yss.filesys.application.port;

import com.yss.filesys.application.dto.PageDTO;
import com.yss.filesys.application.dto.SubscriptionPlanDTO;
import com.yss.filesys.application.query.SubscriptionPlanPageQuery;

public interface SubscriptionPlanQueryUseCase {

    PageDTO<SubscriptionPlanDTO> page(SubscriptionPlanPageQuery query);

    SubscriptionPlanDTO detail(Long id);

    SubscriptionPlanDTO getByCode(String planCode);

    SubscriptionPlanDTO getByName(String planName);

    SubscriptionPlanDTO getDefaultPlan();
}
