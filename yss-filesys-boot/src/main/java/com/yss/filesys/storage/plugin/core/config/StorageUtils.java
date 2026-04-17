package com.yss.filesys.storage.plugin.core.config;

import org.springframework.util.StringUtils;

public final class StorageUtils {

    public static final String LOCAL_PLATFORM_IDENTIFIER = "Local";

    private StorageUtils() {
    }

    public static boolean isLocalConfig(String configId) {
        return configId == null || configId.isBlank() || LOCAL_PLATFORM_IDENTIFIER.equals(configId);
    }

    public static String normalizeConfigId(String configId) {
        return isLocalConfig(configId) ? null : configId;
    }

    public static String generateCacheKey(String platformIdentifier, String configId) {
        if (isLocalConfig(configId)) {
            return "local:system";
        }
        if (!StringUtils.hasText(platformIdentifier)) {
            throw new IllegalArgumentException("平台标识符不能为空");
        }
        return configId + ":" + platformIdentifier;
    }
}
