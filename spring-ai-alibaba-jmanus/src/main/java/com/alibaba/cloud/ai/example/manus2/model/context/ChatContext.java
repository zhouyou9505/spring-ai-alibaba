package com.alibaba.cloud.ai.example.manus2.model.context;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ChatContext extends Context {
    private List<Map<String, Object>> messages;

    public ChatContext() {
        setType("chat");
    }
}
