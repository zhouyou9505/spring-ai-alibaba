package com.alibaba.cloud.ai.example.manus2.model.enums;

public enum ErrorType {
    FATAL("fatal"),
    ESCALATE("escalate");

    private final String value;

    ErrorType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
} 