package com.alibaba.cloud.ai.example.manus2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class Project {
    @JsonProperty("_id")
    private String id;
    private String name;
    private String createdAt;
    private String lastUpdatedAt;
    private String createdByUserId;
    private String secret;
    private String chatClientId;
    private String webhookUrl;
    private String publishedWorkflowId;
    private Integer nextWorkflowNumber;
    private Integer testRunCounter;
    private List<MCPServer> mcpServers;
}

@Data
class MCPServer {
    private String id;
    private String name;
    private String url;
    private String apiKey;
    private boolean enabled;
    private String status;
    private String lastCheckedAt;
    private Map<String, Object> metadata;
} 