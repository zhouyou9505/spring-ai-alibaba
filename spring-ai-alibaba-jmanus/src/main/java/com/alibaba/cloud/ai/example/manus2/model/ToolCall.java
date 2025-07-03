package com.alibaba.cloud.ai.example.manus2.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "tool_calls")
public class ToolCall {
    @Id
    private String id;

    @Indexed
    private String sessionId;

    @Indexed
    private String agentId;

    private String toolName;
    private Map<String, Object> parameters;
    private Map<String, Object> result;
    private String status; // pending, success, error
    private String errorMessage;
    private String errorCode;
    private int tokenCount;
    private long executionTimeMs;
    private Map<String, Object> metadata;

    @CreatedDate
    private Instant createdAt;

    private Instant completedAt;

    private String type;
    private Function function;
    private Map<String, Object> additionalProperties;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Function {
        private String name;
        private String arguments;
        private Map<String, Object> additionalProperties;
    }

    public void markAsSuccess(Map<String, Object> result, long executionTimeMs) {
        this.status = "success";
        this.result = result;
        this.executionTimeMs = executionTimeMs;
        this.completedAt = Instant.now();
    }

    public void markAsError(String errorMessage, String errorCode, long executionTimeMs) {
        this.status = "error";
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        this.executionTimeMs = executionTimeMs;
        this.completedAt = Instant.now();
    }
} 