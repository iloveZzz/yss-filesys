package com.yss.filesys.feignsdk.autoconfigure;

import com.yss.filesys.feignsdk.client.YssFilesysTransferFeignClient;
import com.yss.filesys.feignsdk.service.YssFilesysTransferSdkService;
import com.yss.filesys.feignsdk.properties.YssFilesysFeignSdkProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * yss-filesys Feign SDK 自动配置。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "yss.filesys.feignsdk", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(YssFilesysFeignSdkProperties.class)
@EnableFeignClients(clients = YssFilesysTransferFeignClient.class)
@ComponentScan(basePackageClasses = YssFilesysTransferSdkService.class)
public class YssFilesysFeignSdkAutoConfiguration {
}
