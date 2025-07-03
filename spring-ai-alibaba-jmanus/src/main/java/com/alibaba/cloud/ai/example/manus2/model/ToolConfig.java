package com.alibaba.cloud.ai.example.manus2.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolConfig {
    private String name;
    private String description;
    private String type;
    private Map<String, Object> parameters;
    private Map<String, Object> metadata;
    private Map<String, Object> additionalProperties;
} 