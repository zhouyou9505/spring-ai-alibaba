package com.alibaba.cloud.ai.example.manus2.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
@Document(collection = "messages")
public class Message {
    @Id
    private String id;

    @Indexed
    private String sessionId;

    @Indexed
    private String agentId;

    private String role; // user, assistant, system, tool
    private String sender;
    private String content;
    private String name; // for tool calls
    
    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls = new ArrayList<>();
    
    private Map<String, Object> toolCallResults;
    private Map<String, Object> metadata;
    
    @JsonIgnore
    private int tokenCount;
    
    @JsonIgnore
    private boolean error;
    
    @JsonIgnore
    private String errorMessage;
    
    @JsonIgnore
    private String errorCode;
    
    private LocalDateTime createdAt;
    private Long timestamp;
    
    @JsonProperty("tool_call_id")
    private String toolCallId = "";
    
    @JsonProperty("tool_name")
    private String toolName = "";
    
    @JsonProperty("response_type")
    private String responseType;
    
    private Map<String, Object> additionalProperties;

    @CreatedDate
    private Instant createdAtInstant;

    public static Message userMessage(String sessionId, String agentId, String content) {
        Message message = new Message();
        message.setSessionId(sessionId);
        message.setAgentId(agentId);
        message.setRole("user");
        message.setContent(content);
        return message;
    }

    public static Message assistantMessage(String sessionId, String agentId, String content) {
        Message message = new Message();
        message.setSessionId(sessionId);
        message.setAgentId(agentId);
        message.setRole("assistant");
        message.setContent(content);
        return message;
    }

    public static Message systemMessage(String sessionId, String agentId, String content) {
        Message message = new Message();
        message.setSessionId(sessionId);
        message.setAgentId(agentId);
        message.setRole("system");
        message.setContent(content);
        return message;
    }

    public static Message toolMessage(String sessionId, String agentId, String name, String content) {
        Message message = new Message();
        message.setSessionId(sessionId);
        message.setAgentId(agentId);
        message.setRole("tool");
        message.setName(name);
        message.setContent(content);
        return message;
    }

    public static Message errorMessage(String sessionId, String agentId, String errorMessage, String errorCode) {
        Message message = new Message();
        message.setSessionId(sessionId);
        message.setAgentId(agentId);
        message.setRole("system");
        message.setError(true);
        message.setErrorMessage(errorMessage);
        message.setErrorCode(errorCode);
        return message;
    }

    // Helper methods
    @JsonIgnore
    public boolean isAgentTransferMessage() {
        if ("assistant".equals(role) && content == null && toolCalls != null && !toolCalls.isEmpty()) {
            ToolCall firstCall = toolCalls.get(0);
            return firstCall != null && firstCall.getFunction() != null && 
                   "transfer_to_agent".equals(firstCall.getFunction().getName());
        }
        if ("tool".equals(role) && toolCalls == null && toolCallId != null && "transfer_to_agent".equals(toolName)) {
            return true;
        }
        return false;
    }

    public void setDefaultContent() {
        if ("assistant".equals(role) && content == null && toolCalls != null && !toolCalls.isEmpty()) {
            content = "Calling tool";
        }
    }

    public void setDefaultRole() {
        if ("tool".equals(role)) {
            role = "developer";
        } else if (role == null) {
            role = "user";
        }
    }
} 