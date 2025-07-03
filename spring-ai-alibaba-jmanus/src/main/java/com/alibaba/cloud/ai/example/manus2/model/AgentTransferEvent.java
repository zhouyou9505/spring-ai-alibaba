package com.alibaba.cloud.ai.example.manus2.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentTransferEvent {
    private AgentConfig newAgent;
    private String reason;
} 