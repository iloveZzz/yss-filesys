package com.yss.filesys.storage.plugin.core.config;

import com.yss.filesys.domain.model.BizException;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class StorageConfig {

    String configId;
    String platformIdentifier;
    String userId;
    Map<String, Object> properties;
    Boolean enabled;
    String remark;

    public String getCacheKey() {
        return StorageUtils.generateCacheKey(platformIdentifier, configId);
    }

    public boolean isLocal() {
        return StorageUtils.isLocalConfig(configId);
    }

    public void validate() {
        if (platformIdentifier == null || platformIdentifier.isBlank()) {
            throw new BizException("平台标识符不能为空");
        }
        if (!isLocal() && (userId == null || userId.isBlank())) {
            throw new BizException("用户ID不能为空");
        }
        if (properties == null) {
            throw new BizException("配置属性不能为空");
        }
    }
}
