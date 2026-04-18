package com.yss.filesys.application.form;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 示例表单模板实现。
 * <p>
 * 展示如何基于 {@link FormTemplate} 抽象类实现具体的表单模板，
 * 包含流程审批表单的常见字段配置。
 * </p>
 */
@Component
public class DemoFormTemplate extends FormTemplate {

    @Override
    public String getName() {
        return "das_form";
    }

    @Override
    public Map<String, Object> initialValues() {
        Map<String, Object> initialValues = new LinkedHashMap<>();
        initialValues.put("enabled", Boolean.TRUE);
        initialValues.put("processName", "测试");
        initialValues.put("approvalMode", "SINGLE");
        initialValues.put("description", "测试");
        initialValues.put("processType", "测试流程类型");
        initialValues.put("approverType", "asdsad");
        return initialValues;
    }

    @Override
    public String getDescription() {
        return "数据服务表单";
    }

    @Override
    public String getCategory() {
        return "das";
    }

    @Override
    public YssFormilyDsl.YssFormDefinition buildForm() {
        return YssFormilyDsl.form()
                .mode(YssFormilyDsl.Mode.EDIT)
                .horizontal(120)
                .gridDefaults(1, 1, 230, 16, 0)
                .initialValues(initialValues())
                .components("YMonaco")
                .detailOption("bordered", true)
                .detailOption("maxColumns", 1)
                .nodes(
                        YssFormilyDsl.input("processName", "流程名称")
                                .required()
                                .placeholder("请输入流程名称")
                                .tooltip("流程名称用于审批发起时展示"),

                        YssFormilyDsl.select("processType", "流程类型")
                                .required()
                                .placeholder("请选择流程类型"),

                        YssFormilyDsl.radioGroup("approvalMode", "审批模式")
                                .required()
                                .options(
                                        YssFormilyDsl.option("SINGLE", "单人审批"),
                                        YssFormilyDsl.option("PARALLEL", "并行审批"),
                                        YssFormilyDsl.option("SEQUENTIAL", "串行审批")
                                ),

                        YssFormilyDsl.select("approverType", "审批人类型")
                                .placeholder("请选择审批人类型")
                                .reaction(
                                        YssFormilyDsl.reaction()
                                                .dependencies("processType", "approvalMode")
                                                .when("{{ $deps[0] === 'LEAVE' && $deps[1] !== 'SINGLE' }}")
                                                .fulfillState("visible", true)
                                                .fulfillState("required", true)
                                                .otherwiseState("visible", false)
                                                .otherwiseState("required", false)
                                ),

                        YssFormilyDsl.switchField("enabled", "是否启用")
                                .componentProp("checkedChildren", "启用")
                                .componentProp("unCheckedChildren", "停用"),

                        YssFormilyDsl.textArea("description", "流程说明")
                                .placeholder("请输入流程说明")
                                .componentProp("rows", 4)
                                .gridSpan(2)
                                .disabledExpr("{{ $values.enabled === false }}")
                )
                .build();

    }
}
