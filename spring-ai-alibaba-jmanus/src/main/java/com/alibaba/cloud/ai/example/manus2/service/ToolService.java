package com.alibaba.cloud.ai.example.manus2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.alibaba.cloud.ai.example.manus2.model.ToolConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class ToolService {
    
    private final ObjectMapper objectMapper;
    
    public ToolService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    public String executeTool(String toolName, String arguments, ToolConfig toolConfig, Map<String, Object> completeRequest) {
        try {
            log.info("Executing tool: {} with arguments: {}", toolName, arguments);
            
            // Check if this is a mock tool
            if (isMockTool(toolConfig, completeRequest)) {
                return executeMockTool(toolName, arguments, toolConfig, completeRequest);
            }
            
            // Check if this is an MCP tool
            if (isMcpTool(toolConfig)) {
                return executeMcpTool(toolName, arguments, toolConfig, completeRequest);
            }
            
            // Default to webhook
            return executeWebhookTool(toolName, arguments, toolConfig, completeRequest);
            
        } catch (Exception e) {
            log.error("Error executing tool {}: {}", toolName, e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }
    
    private boolean isMockTool(ToolConfig toolConfig, Map<String, Object> completeRequest) {
        if (toolConfig.getMetadata() != null && toolConfig.getMetadata().containsKey("mockTool")) {
            return (Boolean) toolConfig.getMetadata().get("mockTool");
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> testProfile = (Map<String, Object>) completeRequest.get("testProfile");
        if (testProfile != null && testProfile.containsKey("mockTools")) {
            return (Boolean) testProfile.get("mockTools");
        }
        
        return false;
    }
    
    private boolean isMcpTool(ToolConfig toolConfig) {
        return toolConfig.getMetadata() != null && toolConfig.getMetadata().containsKey("isMcp") &&
               (Boolean) toolConfig.getMetadata().get("isMcp");
    }
    
    private String executeMockTool(String toolName, String arguments, ToolConfig toolConfig, Map<String, Object> completeRequest) {
        try {
            String description = toolConfig.getDescription() != null ? toolConfig.getDescription() : "";
            String mockInstructions = getMockInstructions(toolConfig, completeRequest);
            
            // In a real implementation, you would call an AI model to generate a realistic response
            // For now, we'll return a simple mock response
            return String.format("Mock response for tool '%s' with arguments: %s. Description: %s", 
                    toolName, arguments, description);
                    
        } catch (Exception e) {
            log.error("Error in mock tool execution: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }
    
    private String getMockInstructions(ToolConfig toolConfig, Map<String, Object> completeRequest) {
        @SuppressWarnings("unchecked")
        Map<String, Object> testProfile = (Map<String, Object>) completeRequest.get("testProfile");
        
        if (testProfile != null && testProfile.containsKey("mockPrompt")) {
            return (String) testProfile.get("mockPrompt");
        }
        
        if (toolConfig.getMetadata() != null && toolConfig.getMetadata().containsKey("mockInstructions")) {
            return (String) toolConfig.getMetadata().get("mockInstructions");
        }
        
        return "Generate a realistic response for this tool.";
    }
    
    private String executeMcpTool(String toolName, String arguments, ToolConfig toolConfig, Map<String, Object> completeRequest) {
        try {
            String mcpServerUrl = getMcpServerUrl(toolConfig, completeRequest);
            if (mcpServerUrl == null || mcpServerUrl.isEmpty()) {
                return "Error: MCP server URL not configured";
            }
            
            // In a real implementation, you would connect to the MCP server and execute the tool
            // For now, we'll return a placeholder response
            return String.format("MCP tool '%s' would be executed on server: %s with arguments: %s", 
                    toolName, mcpServerUrl, arguments);
                    
        } catch (Exception e) {
            log.error("Error in MCP tool execution: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }
    
    private String getMcpServerUrl(ToolConfig toolConfig, Map<String, Object> completeRequest) {
        if (toolConfig.getMetadata() != null && toolConfig.getMetadata().containsKey("mcpServerURL")) {
            return (String) toolConfig.getMetadata().get("mcpServerURL");
        }
        
        // Backwards compatibility for old projects
        if (toolConfig.getMetadata() != null && toolConfig.getMetadata().containsKey("mcpServerName")) {
            String mcpServerName = (String) toolConfig.getMetadata().get("mcpServerName");
            @SuppressWarnings("unchecked")
            Map<String, Object> mcpServers = (Map<String, Object>) completeRequest.get("mcpServers");
            
            if (mcpServers != null) {
                // This is a simplified lookup - in a real implementation you'd iterate through the servers
                return (String) mcpServers.get(mcpServerName);
            }
        }
        
        return null;
    }
    
    private String executeWebhookTool(String toolName, String arguments, ToolConfig toolConfig, Map<String, Object> completeRequest) {
        try {
            String webhookUrl = (String) completeRequest.get("toolWebhookUrl");
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                return "Error: Webhook URL not configured";
            }
            
            // In a real implementation, you would make an HTTP POST request to the webhook
            // For now, we'll return a placeholder response
            return String.format("Webhook tool '%s' would be called at: %s with arguments: %s", 
                    toolName, webhookUrl, arguments);
                    
        } catch (Exception e) {
            log.error("Error in webhook tool execution: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }
} 