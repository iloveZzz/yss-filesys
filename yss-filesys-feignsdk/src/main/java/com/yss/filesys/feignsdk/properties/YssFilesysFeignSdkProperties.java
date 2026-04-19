package com.yss.filesys.feignsdk.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * yss-filesys Feign SDK 配置。
 */
@Data
@ConfigurationProperties(prefix = "yss.filesys.feignsdk")
public class YssFilesysFeignSdkProperties {

    /**
     * 是否启用 SDK 自动配置。
     */
    private boolean enabled = true;

    /**
     * yss-filesys 服务名。
     */
    private String name = "yss-filesys";

    /**
     * yss-filesys 统一路径前缀。
     */
    private String path = "/transfers";

    /**
     * 默认分片大小，单位字节。
     */
    private long defaultChunkSize = 5L * 1024 * 1024;
}
