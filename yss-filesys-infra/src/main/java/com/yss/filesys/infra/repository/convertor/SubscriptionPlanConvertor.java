package com.yss.filesys.infra.repository.convertor;

import com.yss.filesys.domain.model.SubscriptionPlan;
import com.yss.filesys.infra.repository.entity.SubscriptionPlanPO;

public final class SubscriptionPlanConvertor {

    private SubscriptionPlanConvertor() {
    }

    public static SubscriptionPlan toDomain(SubscriptionPlanPO po) {
        if (po == null) {
            return null;
        }
        return SubscriptionPlan.builder()
                .id(po.getId())
                .planCode(po.getPlanCode())
                .planName(po.getPlanName())
                .description(po.getDescription())
                .storageQuotaGb(po.getStorageQuotaGb())
                .maxFiles(po.getMaxFiles())
                .maxFileSize(po.getMaxFileSize())
                .bandwidthQuota(po.getBandwidthQuota())
                .price(po.getPrice())
                .isActive(po.getIsActive())
                .isDefault(po.getIsDefault())
                .sortOrder(po.getSortOrder())
                .deleted(po.getDeleted())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    public static SubscriptionPlanPO toPO(SubscriptionPlan domain) {
        if (domain == null) {
            return null;
        }
        SubscriptionPlanPO po = new SubscriptionPlanPO();
        po.setId(domain.getId());
        po.setPlanCode(domain.getPlanCode());
        po.setPlanName(domain.getPlanName());
        po.setDescription(domain.getDescription());
        po.setStorageQuotaGb(domain.getStorageQuotaGb());
        po.setMaxFiles(domain.getMaxFiles());
        po.setMaxFileSize(domain.getMaxFileSize());
        po.setBandwidthQuota(domain.getBandwidthQuota());
        po.setPrice(domain.getPrice());
        po.setIsActive(domain.getIsActive());
        po.setIsDefault(domain.getIsDefault());
        po.setSortOrder(domain.getSortOrder());
        po.setDeleted(domain.getDeleted());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }
}
