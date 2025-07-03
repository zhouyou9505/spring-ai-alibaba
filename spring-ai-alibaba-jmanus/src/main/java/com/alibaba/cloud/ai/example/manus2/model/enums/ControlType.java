package com.alibaba.cloud.ai.example.manus2.model.enums;

public enum ControlType {
    RETAIN("retain"),
    PARENT_AGENT("relinquish_to_parent"),
    START_AGENT("start_agent");

    private final String value;

    ControlType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
} 