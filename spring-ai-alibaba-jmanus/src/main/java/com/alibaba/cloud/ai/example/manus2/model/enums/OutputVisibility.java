package com.alibaba.cloud.ai.example.manus2.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OutputVisibility {
    USER_FACING("user_facing"),
    INTERNAL("internal");

    private final String value;

    OutputVisibility(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static OutputVisibility fromValue(String value) {
        for (OutputVisibility visibility : OutputVisibility.values()) {
            if (visibility.value.equals(value)) {
                return visibility;
            }
        }
        throw new IllegalArgumentException("Unknown OutputVisibility value: " + value);
    }
} 