package com.alibaba.cloud.ai.example.manus2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class DataSource {
    @JsonProperty("_id")
    private String id;
    private String name;
    private String description;
    private boolean active = true;
    private String status; // 'pending' | 'ready' | 'error' | 'deleted'
    private String error;
    @JsonProperty("data")
    private Map<String, Object> data; // The discriminated union based on type
} 