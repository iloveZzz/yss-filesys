package com.yss.filesys.application.form;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.filesys.application.dto.StorageFormTemplateDTO;
import com.yss.filesys.domain.model.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StorageFormTemplateRegistry {

    private final List<FormTemplate> formTemplates;
    private final ObjectMapper objectMapper;
    private Map<String, FormTemplate> templateMap;

    public synchronized void initialize() {
        Map<String, FormTemplate> map = new LinkedHashMap<>();
        List<FormTemplate> orderedTemplates = formTemplates.stream()
                .sorted(Comparator.comparingInt(this::templateOrder).thenComparing(FormTemplate::getName))
                .toList();
        for (FormTemplate template : orderedTemplates) {
            map.put(template.getName(), template);
        }
        this.templateMap = map;
    }

    public Collection<StorageFormTemplateDTO> listTemplates() {
        ensureInitialized();
        return templateMap.values().stream().map(this::toDTO).toList();
    }

    public Optional<StorageFormTemplateDTO> getTemplate(String identifier) {
        ensureInitialized();
        return Optional.ofNullable(templateMap.get(identifier)).map(this::toDTO);
    }

    public Optional<String> getSchemaJson(String identifier) {
        ensureInitialized();
        FormTemplate template = templateMap.get(identifier);
        if (template == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.writeValueAsString(template.buildForm().toMap()));
        } catch (Exception e) {
            throw new BizException("生成存储表单 schema 失败: " + e.getMessage());
        }
    }

    private StorageFormTemplateDTO toDTO(FormTemplate template) {
        return StorageFormTemplateDTO.builder()
                .identifier(template.getName())
                .name(template.getName())
                .description(template.getDescription())
                .category(template.getCategory())
                .version(template.getVersion())
                .formSchema(template.buildForm().toMap())
                .initialValues(template.initialValues())
                .build();
    }

    private void ensureInitialized() {
        if (templateMap == null) {
            initialize();
        }
    }

    private int templateOrder(FormTemplate template) {
        return switch (template.getName()) {
            case "local" -> 0;
            case "S3" -> 1;
            case "OSS" -> 2;
            case "COS" -> 3;
            default -> 100;
        };
    }
}
