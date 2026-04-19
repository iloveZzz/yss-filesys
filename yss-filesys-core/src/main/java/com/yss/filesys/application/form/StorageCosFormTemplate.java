package com.yss.filesys.application.form;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class StorageCosFormTemplate extends FormTemplate {

    @Override
    public String getName() {
        return "COS";
    }

    @Override
    public Map<String, Object> initialValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("prefix", "");
        return values;
    }

    @Override
    public String getDescription() {
        return "腾讯云 COS 存储配置";
    }

    @Override
    public String getCategory() {
        return "storage";
    }

    @Override
    public YssFormilyDsl.YssFormDefinition buildForm() {
        return YssFormilyDsl.form()
                .mode(YssFormilyDsl.Mode.EDIT)
                .horizontal(120)
                .gridDefaults(1, 1, 260, 16, 0)
                .initialValues(initialValues())
                .detailOption("bordered", true)
                .detailOption("maxColumns", 1)
                .nodes(
                        YssFormilyDsl.input("bucket", "Bucket")
                                .required()
                                .placeholder("例如：my-bucket-1250000000")
                                .gridSpan(1),
                        YssFormilyDsl.input("region", "Region")
                                .required()
                                .placeholder("例如：ap-guangzhou")
                                .gridSpan(1),
                        YssFormilyDsl.input("secretId", "Secret Id")
                                .required()
                                .placeholder("请输入 Secret Id")
                                .gridSpan(1),
                        YssFormilyDsl.input("secretKey", "Secret Key")
                                .required()
                                .placeholder("请输入 Secret Key")
                                .gridSpan(1),
                        YssFormilyDsl.input("endpoint", "Endpoint")
                                .placeholder("例如：https://cos.ap-guangzhou.myqcloud.com")
                                .gridSpan(1),
                        YssFormilyDsl.input("prefix", "前缀")
                                .placeholder("可选，例如：tenant-a/")
                                .gridSpan(1)
                )
                .build();
    }
}
