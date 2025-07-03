package com.alibaba.cloud.ai.example.manus2.repository;

import com.alibaba.cloud.ai.example.manus2.model.Agent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentRepository extends MongoRepository<Agent, String> {
} 