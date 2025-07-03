package com.alibaba.cloud.ai.example.manus2.model.context;

import lombok.Data;

@Data
public class AgentContext extends Context {
    private String agentName;

    public AgentContext() {
        setType("agent");
    }
}
