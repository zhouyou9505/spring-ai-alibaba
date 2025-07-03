package com.alibaba.cloud.ai.example.manus2.model.context;

import lombok.Data;

@Data
public class ToolContext extends Context {
    private String toolName;

    public ToolContext() {
        setType("tool");
    }
}
