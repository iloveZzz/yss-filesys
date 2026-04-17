package com.yss.filesys.application.query;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class SubscriptionPlanPageQuery {

    @Min(value = 1, message = "pageNo必须大于0")
    private Long pageNo = 1L;

    @Min(value = 1, message = "pageSize必须大于0")
    private Long pageSize = 10L;

    private String planName;
}
