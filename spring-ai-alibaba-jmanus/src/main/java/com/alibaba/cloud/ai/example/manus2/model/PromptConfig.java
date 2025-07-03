package com.alibaba.cloud.ai.example.manus2.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.alibaba.cloud.ai.example.manus2.model.enums.PromptType;
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
public class PromptConfig {
    private String name;
    private PromptType type;
    private String content;
    private Map<String, Object> metadata;
    private Map<String, Object> additionalProperties;
} 