package com.alibaba.cloud.ai.example.manus2.model.enums;

public enum ResponseType {
    INTERNAL("internal"),
    EXTERNAL("external");

    private final String value;

    ResponseType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
} 