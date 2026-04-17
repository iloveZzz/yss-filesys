package com.yss.filesys.application.port;

import com.yss.filesys.application.command.SubscriptionPlanAddCommand;
import com.yss.filesys.application.command.SubscriptionPlanEditCommand;

public interface SubscriptionPlanCommandUseCase {

    void add(SubscriptionPlanAddCommand command);

    void edit(SubscriptionPlanEditCommand command);

    void delete(Long id);
}
