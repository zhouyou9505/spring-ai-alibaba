package com.alibaba.cloud.ai.example.manus2.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "agents")
public class Agent {
    @Id
    private String id;

    private String configId;
    private String name;
    private String description;
    private String instructions;
    private String model;
    private List<String> tools;
    private List<Agent> handoffs;
    private Map<String, Object> toolConfigs;
    private Map<String, Object> modelConfigs;
    private boolean ragEnabled;
    private int maxCallsPerTurn;
    private int maxTokensPerTurn;
    private int maxTokensPerResponse;
    private double temperature;
    private boolean visible;
    private Map<String, Object> metadata;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private Instant lastActiveAt;
    private int totalCalls;
    private int totalTokens;
    private Map<String, Integer> toolUsage;
    private List<String> recentMessages;
    private Map<String, Object> state;
} 