package com.alibaba.cloud.ai.example.manus2.model.enums;

public enum AgentRole {
    ESCALATION("escalation"),
    POST_PROCESSING("post_process"),
    GUARDRAILS("guardrails");

    private final String value;

    AgentRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
} 