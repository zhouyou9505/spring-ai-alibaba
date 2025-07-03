package com.alibaba.cloud.ai.example.manus2.model.context;

import lombok.Data;

@Data
public class PromptContext extends Context {
    private String promptName;

    public PromptContext() {
        setType("prompt");
    }
}
