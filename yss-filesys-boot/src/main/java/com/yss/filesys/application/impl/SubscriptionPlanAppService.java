package com.yss.filesys.application.impl;

import com.yss.filesys.application.command.SubscriptionPlanAddCommand;
import com.yss.filesys.application.command.SubscriptionPlanEditCommand;
import com.yss.filesys.application.dto.PageDTO;
import com.yss.filesys.application.dto.SubscriptionPlanDTO;
import com.yss.filesys.application.port.SubscriptionPlanCommandUseCase;
import com.yss.filesys.application.port.SubscriptionPlanQueryUseCase;
import com.yss.filesys.application.query.SubscriptionPlanPageQuery;
import com.yss.filesys.domain.gateway.SubscriptionPlanGateway;
import com.yss.filesys.domain.gateway.UserSubscriptionGateway;
import com.yss.filesys.domain.model.BizException;
import com.yss.filesys.domain.model.SubscriptionPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanAppService implements SubscriptionPlanCommandUseCase, SubscriptionPlanQueryUseCase {

    private final SubscriptionPlanGateway subscriptionPlanGateway;
    private final UserSubscriptionGateway userSubscriptionGateway;

    @Override
    public PageDTO<SubscriptionPlanDTO> page(SubscriptionPlanPageQuery query) {
        PageDTO<SubscriptionPlan> page = subscriptionPlanGateway.page(query);
        return PageDTO.<SubscriptionPlanDTO>builder()
                .total(page.getTotal())
                .pageIndex(page.getPageIndex())
                .pageSize(page.getPageSize())
                .records(page.getRecords().stream().map(this::toDTO).toList())
                .build();
    }

    @Override
    public SubscriptionPlanDTO detail(Long id) {
        return toDTO(findPlan(id));
    }

    @Override
    public SubscriptionPlanDTO getByCode(String planCode) {
        return toDTO(subscriptionPlanGateway.findByCode(planCode)
                .orElseThrow(() -> new BizException("当前套餐不存在: " + planCode)));
    }

    @Override
    public SubscriptionPlanDTO getByName(String planName) {
        return toDTO(subscriptionPlanGateway.findByName(planName)
                .orElseThrow(() -> new BizException("当前套餐不存在: " + planName)));
    }

    @Override
    public SubscriptionPlanDTO getDefaultPlan() {
        return subscriptionPlanGateway.findDefaultPlan()
                .map(this::toDTO)
                .orElseThrow(() -> new BizException("未找到默认套餐"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(SubscriptionPlanAddCommand command) {
        ensureUnique(command.getPlanCode(), command.getPlanName(), null);
        LocalDateTime now = LocalDateTime.now();
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .planCode(command.getPlanCode())
                .planName(command.getPlanName())
                .description(command.getDescription())
                .storageQuotaGb(command.getStorageQuotaGb())
                .maxFiles(command.getMaxFiles())
                .maxFileSize(command.getMaxFileSize())
                .bandwidthQuota(command.getBandwidthQuota())
                .price(command.getPrice())
                .isActive(command.getIsActive())
                .isDefault(command.getIsDefault())
                .sortOrder(command.getSortOrder())
                .deleted(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        subscriptionPlanGateway.save(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(SubscriptionPlanEditCommand command) {
        SubscriptionPlan current = findPlan(command.getId());
        ensureUnique(command.getPlanCode(), command.getPlanName(), current.getId());
        SubscriptionPlan plan = current.toBuilder()
                .planCode(command.getPlanCode())
                .planName(command.getPlanName())
                .description(command.getDescription())
                .storageQuotaGb(command.getStorageQuotaGb())
                .maxFiles(command.getMaxFiles())
                .maxFileSize(command.getMaxFileSize())
                .bandwidthQuota(command.getBandwidthQuota())
                .price(command.getPrice())
                .isActive(command.getIsActive())
                .isDefault(command.getIsDefault())
                .sortOrder(command.getSortOrder())
                .updatedAt(LocalDateTime.now())
                .build();
        subscriptionPlanGateway.save(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (userSubscriptionGateway.hasActiveByPlanId(id)) {
            throw new BizException("当前套餐还存在生效的订阅，无法删除！");
        }
        findPlan(id);
        subscriptionPlanGateway.deleteById(id);
    }

    private SubscriptionPlan findPlan(Long id) {
        return subscriptionPlanGateway.findById(id)
                .orElseThrow(() -> new BizException("当前套餐不存在: " + id));
    }

    private void ensureUnique(String planCode, String planName, Long currentId) {
        subscriptionPlanGateway.findByCode(planCode)
                .filter(plan -> currentId == null || !currentId.equals(plan.getId()))
                .ifPresent(plan -> {
                    throw new BizException("当前套餐编码已存在！");
                });
        subscriptionPlanGateway.findByName(planName)
                .filter(plan -> currentId == null || !currentId.equals(plan.getId()))
                .ifPresent(plan -> {
                    throw new BizException("当前套餐名称已存在！");
                });
    }

    private SubscriptionPlanDTO toDTO(SubscriptionPlan plan) {
        return SubscriptionPlanDTO.builder()
                .id(plan.getId())
                .planCode(plan.getPlanCode())
                .planName(plan.getPlanName())
                .description(plan.getDescription())
                .storageQuotaGb(plan.getStorageQuotaGb())
                .maxFiles(plan.getMaxFiles())
                .maxFileSize(plan.getMaxFileSize())
                .bandwidthQuota(plan.getBandwidthQuota())
                .price(plan.getPrice())
                .isActive(plan.getIsActive())
                .isDefault(plan.getIsDefault())
                .sortOrder(plan.getSortOrder())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }
}
