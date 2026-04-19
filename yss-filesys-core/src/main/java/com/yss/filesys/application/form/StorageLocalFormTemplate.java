package com.yss.filesys.application.form;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class StorageLocalFormTemplate extends FormTemplate {

    @Override
    public String getName() {
        return "local";
    }

    @Override
    public Map<String, Object> initialValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("storageRoot", "/tmp/yss-filesys/storage");
        return values;
    }

    @Override
    public String getDescription() {
        return "本地文件系统存储配置";
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
                        YssFormilyDsl.input("storageRoot", "存储根目录")
                                .required()
                                .placeholder("/tmp/yss-filesys/storage")
                                .tooltip("本地存储文件根目录")
                                .gridSpan(1)
                )
                .build();
    }
}
