package com.alibaba.cloud.ai.example.manus2.model.enums;

public enum PromptType {
    STYLE("style_prompt"),
    GREETING("greeting");

    private final String value;

    PromptType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
} 