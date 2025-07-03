package com.alibaba.cloud.ai.example.manus2.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "sessions")
public class Session {
    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String agentId;

    private String title;
    private List<Message> messages;
    private Map<String, Object> context;
    private Map<String, Object> metadata;
    private int totalMessages;
    private int totalTokens;
    private Instant lastMessageAt;
    private boolean active;
    private String status; // active, paused, completed, error
    private String errorMessage;
    private String errorCode;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public void addMessage(Message message) {
        messages.add(message);
        totalMessages++;
        totalTokens += message.getTokenCount();
        lastMessageAt = Instant.now();
    }

    public void markAsError(String errorMessage, String errorCode) {
        this.status = "error";
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        this.active = false;
    }

    public void markAsCompleted() {
        this.status = "completed";
        this.active = false;
    }

    public void pause() {
        this.status = "paused";
        this.active = false;
    }

    public void resume() {
        this.status = "active";
        this.active = true;
    }
} 