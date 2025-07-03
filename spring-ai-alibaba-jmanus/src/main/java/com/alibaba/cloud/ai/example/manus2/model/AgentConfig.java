package com.alibaba.cloud.ai.example.manus2.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.alibaba.cloud.ai.example.manus2.model.enums.AgentRole;
import com.alibaba.cloud.ai.example.manus2.model.enums.OutputVisibility;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "agent_configs")
public class AgentConfig {
    @Id
    private String id;

    private String name;
    private String description;
    private String instructions;
    private String model;
    private OutputVisibility outputVisibility;
    private AgentRole role;
    private List<String> tools;
    private List<String> connectedAgents;
    private Map<String, Object> toolConfigs;
    private Map<String, Object> modelConfigs;
    private boolean ragEnabled;
    private int maxCallsPerTurn;
    private int maxTokensPerTurn;
    private int maxTokensPerResponse;
    private double temperature;
    private boolean visible;
    private Map<String, Object> metadata;
    private Map<String, Object> additionalProperties;
} 