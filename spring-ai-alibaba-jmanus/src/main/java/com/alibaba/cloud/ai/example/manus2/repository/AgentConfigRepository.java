package com.alibaba.cloud.ai.example.manus2.repository;

import com.alibaba.cloud.ai.example.manus2.model.AgentConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentConfigRepository extends MongoRepository<AgentConfig, String> {
    List<AgentConfig> findByVisibleTrue();
    List<AgentConfig> findByNameContainingIgnoreCase(String name);
} 