package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class StorageFormTemplateDTO {
    String identifier;
    String name;
    String description;
    String category;
    String version;
    Map<String, Object> formSchema;
    Map<String, Object> initialValues;
}
