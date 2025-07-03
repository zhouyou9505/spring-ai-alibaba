package com.alibaba.cloud.ai.example.manus2.model;

import lombok.Data;

@Data
public class ProjectMember {
    private String userId;
    private String projectId;
    private String createdAt;
    private String lastUpdatedAt;
} 