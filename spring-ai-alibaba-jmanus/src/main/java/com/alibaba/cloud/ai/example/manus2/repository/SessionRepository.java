package com.alibaba.cloud.ai.example.manus2.repository;

import com.alibaba.cloud.ai.example.manus2.model.Session;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends MongoRepository<Session, String> {
    List<Session> findByAgentIdAndActiveTrue(String agentId);
    List<Session> findByUserIdAndActiveTrue(String userId);
} 