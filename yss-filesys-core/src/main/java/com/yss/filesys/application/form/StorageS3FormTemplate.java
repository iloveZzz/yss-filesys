package com.yss.filesys.application.form;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class StorageS3FormTemplate extends FormTemplate {

    @Override
    public String getName() {
        return "S3";
    }

    @Override
    public Map<String, Object> initialValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("pathStyleEnabled", Boolean.TRUE);
        values.put("prefix", "");
        return values;
    }

    @Override
    public String getDescription() {
        return "S3 协议对象存储配置";
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
                .gridDefaults(2, 1, 260, 16, 0)
                .initialValues(initialValues())
                .detailOption("bordered", true)
                .detailOption("maxColumns", 1)
                .nodes(
                        YssFormilyDsl.groupHeader("base", "基础配置").gridSpan(2),
                        YssFormilyDsl.input("bucket", "Bucket")
                                .required()
                                .placeholder("例如：my-bucket")
                                .gridSpan(1),
                        YssFormilyDsl.input("region", "Region")
                                .required()
                                .placeholder("例如：cn-hangzhou")
                                .gridSpan(1),
                        YssFormilyDsl.input("accessKeyId", "Access Key Id")
                                .required()
                                .placeholder("请输入 Access Key Id")
                                .gridSpan(1),
                        YssFormilyDsl.input("secretAccessKey", "Access Key Secret")
                                .required()
                                .component("Input.Password")
                                .placeholder("请输入 Access Key Secret")
                                .gridSpan(1),
                        YssFormilyDsl.input("endpoint", "Endpoint")
                                .placeholder("https://s3.oss-cn-hangzhou.aliyuncs.com")
                                .gridSpan(2),
                        YssFormilyDsl.switchField("pathStyleEnabled", "Path Style")
                                .componentProp("checkedChildren", "开启")
                                .componentProp("unCheckedChildren", "关闭"),
                        YssFormilyDsl.input("prefix", "前缀")
                                .placeholder("可选，例如：tenant-a/")
                                .gridSpan(2)
                )
                .build();
    }
}
