package com.alibaba.cloud.ai.example.manus2.service;

import com.alibaba.cloud.ai.example.manus2.model.Agent;
import com.alibaba.cloud.ai.example.manus2.model.Message;
import com.alibaba.cloud.ai.example.manus2.model.Session;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AgentsService {
    /**
     * 创建代理实例
     */
    Agent createAgent(String configId);

    /**
     * 获取代理实例
     */
    Optional<Agent> getAgent(String id);

    /**
     * 更新代理状态
     */
    Agent updateAgent(String id, Map<String, Object> state);

    /**
     * 删除代理实例
     */
    void deleteAgent(String id);

    /**
     * 创建对话会话
     */
    Session createSession(String agentId, String userId);

    /**
     * 获取对话会话
     */
    Optional<Session> getSession(String id);

    /**
     * 发送消息到代理
     */
    Message sendMessage(String sessionId, String content);

    /**
     * 获取会话历史消息
     */
    List<Message> getSessionMessages(String sessionId);

    /**
     * 暂停会话
     */
    void pauseSession(String sessionId);

    /**
     * 恢复会话
     */
    void resumeSession(String sessionId);

    /**
     * 结束会话
     */
    void endSession(String sessionId);

    /**
     * 获取代理的活跃会话
     */
    List<Session> getActiveSessions(String agentId);

    /**
     * 获取用户的活跃会话
     */
    List<Session> getUserActiveSessions(String userId);

    /**
     * 更新代理使用统计
     */
    void updateAgentStats(String agentId, Map<String, Integer> stats);

    /**
     * 获取代理使用统计
     */
    Map<String, Integer> getAgentStats(String agentId);
} 