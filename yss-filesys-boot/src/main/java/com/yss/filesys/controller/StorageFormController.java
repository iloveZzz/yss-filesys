package com.yss.filesys.controller;

import com.yss.filesys.application.dto.StorageFormTemplateDTO;
import com.yss.filesys.application.form.StorageFormTemplateRegistry;
import com.yss.cloud.dto.response.MultiResult;
import com.yss.cloud.dto.response.SingleResult;
import com.yss.filesys.domain.model.BizException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/storage/forms")
@Tag(name = "存储表单模板")
@RequiredArgsConstructor
public class StorageFormController {

    private final StorageFormTemplateRegistry storageFormTemplateRegistry;

    @GetMapping
    @Operation(summary = "查询全部存储表单模板")
    public MultiResult<StorageFormTemplateDTO> listStorageFormTemplates() {
        return MultiResult.of(storageFormTemplateRegistry.listTemplates());
    }

    @GetMapping("/{identifier}")
    @Operation(summary = "根据标识查询存储表单模板")
    public SingleResult<StorageFormTemplateDTO> getStorageFormTemplateByIdentifier(@PathVariable String identifier) {
        return SingleResult.of(storageFormTemplateRegistry.getTemplate(identifier)
                .orElseThrow(() -> new BizException("未找到存储表单模板: " + identifier)));
    }
}
