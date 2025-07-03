package com.alibaba.cloud.ai.example.manus2.service.impl;

import com.alibaba.cloud.ai.example.manus2.model.Agent;
import com.alibaba.cloud.ai.example.manus2.model.AgentConfig;
import com.alibaba.cloud.ai.example.manus2.model.Message;
import com.alibaba.cloud.ai.example.manus2.model.Session;
import com.alibaba.cloud.ai.example.manus2.repository.AgentRepository;
import com.alibaba.cloud.ai.example.manus2.repository.SessionRepository;
import com.alibaba.cloud.ai.example.manus2.service.AgentConfigService;
import com.alibaba.cloud.ai.example.manus2.service.AgentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AgentsServiceImpl implements AgentsService {
    private final AgentRepository agentRepository;
    private final SessionRepository sessionRepository;
    private final AgentConfigService agentConfigService;

    @Override
    @Transactional
    public Agent createAgent(String configId) {
        AgentConfig config = agentConfigService.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("Agent config not found"));

        Agent agent = new Agent();
        agent.setConfigId(configId);
        agent.setName(config.getName());
        agent.setDescription(config.getDescription());
        agent.setInstructions(config.getInstructions());
        agent.setModel(config.getModel());
        agent.setTools(config.getTools());
        agent.setToolConfigs(config.getToolConfigs());
        agent.setModelConfigs(config.getModelConfigs());
        agent.setRagEnabled(config.isRagEnabled());
        agent.setMaxCallsPerTurn(config.getMaxCallsPerTurn());
        agent.setMaxTokensPerTurn(config.getMaxTokensPerTurn());
        agent.setMaxTokensPerResponse(config.getMaxTokensPerResponse());
        agent.setTemperature(config.getTemperature());
        agent.setVisible(config.isVisible());
        agent.setMetadata(config.getMetadata());
        agent.setCreatedAt(Instant.now());
        agent.setUpdatedAt(Instant.now());
        agent.setLastActiveAt(Instant.now());
        agent.setTotalCalls(0);
        agent.setTotalTokens(0);
        agent.setToolUsage(new HashMap<>());
        agent.setRecentMessages(new ArrayList<>());
        agent.setState(new HashMap<>());

        return agentRepository.save(agent);
    }

    @Override
    public Optional<Agent> getAgent(String id) {
        return agentRepository.findById(id);
    }

    @Override
    @Transactional
    public Agent updateAgent(String id, Map<String, Object> state) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));

        agent.setState(state);
        agent.setUpdatedAt(Instant.now());
        agent.setLastActiveAt(Instant.now());

        return agentRepository.save(agent);
    }

    @Override
    @Transactional
    public void deleteAgent(String id) {
        agentRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Session createSession(String agentId, String userId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));

        Session session = new Session();
        session.setUserId(userId);
        session.setAgentId(agentId);
        session.setTitle("New Session");
        session.setMessages(new ArrayList<>());
        session.setContext(new HashMap<>());
        session.setMetadata(new HashMap<>());
        session.setTotalMessages(0);
        session.setTotalTokens(0);
        session.setActive(true);
        session.setStatus("active");
        session.setCreatedAt(Instant.now());
        session.setUpdatedAt(Instant.now());

        return sessionRepository.save(session);
    }

    @Override
    public Optional<Session> getSession(String id) {
        return sessionRepository.findById(id);
    }

    @Override
    @Transactional
    public Message sendMessage(String sessionId, String content) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!session.isActive()) {
            throw new IllegalStateException("Session is not active");
        }

        // 创建用户消息
        Message userMessage = Message.userMessage(sessionId, session.getAgentId(), content);
        session.addMessage(userMessage);

        // TODO: 调用 LLM 生成回复
        // 这里需要实现与 LLM 的集成
        Message assistantMessage = Message.assistantMessage(sessionId, session.getAgentId(), "This is a placeholder response");
        session.addMessage(assistantMessage);

        sessionRepository.save(session);
        return assistantMessage;
    }

    @Override
    public List<Message> getSessionMessages(String sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        return session.getMessages();
    }

    @Override
    @Transactional
    public void pauseSession(String sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        session.pause();
        sessionRepository.save(session);
    }

    @Override
    @Transactional
    public void resumeSession(String sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        session.resume();
        sessionRepository.save(session);
    }

    @Override
    @Transactional
    public void endSession(String sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        session.markAsCompleted();
        sessionRepository.save(session);
    }

    @Override
    public List<Session> getActiveSessions(String agentId) {
        return sessionRepository.findByAgentIdAndActiveTrue(agentId);
    }

    @Override
    public List<Session> getUserActiveSessions(String userId) {
        return sessionRepository.findByUserIdAndActiveTrue(userId);
    }

    @Override
    @Transactional
    public void updateAgentStats(String agentId, Map<String, Integer> stats) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));

        Map<String, Integer> toolUsage = new HashMap<>(agent.getToolUsage() != null ? agent.getToolUsage() : new HashMap<>());
        stats.forEach((tool, count) -> 
            toolUsage.merge(tool, count, Integer::sum)
        );

        agent.setToolUsage(toolUsage);
        agent.setUpdatedAt(Instant.now());
        agent.setLastActiveAt(Instant.now());

        agentRepository.save(agent);
    }

    @Override
    public Map<String, Integer> getAgentStats(String agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));
        return agent.getToolUsage();
    }
} 