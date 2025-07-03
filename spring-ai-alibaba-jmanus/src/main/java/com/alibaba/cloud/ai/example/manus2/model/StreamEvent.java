package com.alibaba.cloud.ai.example.manus2.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamEvent {
    private String type;
    private Object data;
} 