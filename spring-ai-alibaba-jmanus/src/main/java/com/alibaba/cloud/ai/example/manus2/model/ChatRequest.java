package com.alibaba.cloud.ai.example.manus2.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRequest {
    private String projectId;
    private List<Message> messages;
    private String startAgent;
    private List<com.alibaba.cloud.ai.example.manus2.model.AgentConfig> agents;
    private List<ToolConfig> tools;
    private List<PromptConfig> prompts;
    private Map<String, Object> state;
    private Boolean enableTracing;
    private List<Map<String, Object>> mcpServers;
    private String toolWebhookUrl;
    private Map<String, Object> additionalProperties;
} 