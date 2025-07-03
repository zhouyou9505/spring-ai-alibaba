package com.alibaba.cloud.ai.example.manus2.service.impl;

import com.alibaba.cloud.ai.example.manus2.model.AgentConfig;
import com.alibaba.cloud.ai.example.manus2.repository.AgentConfigRepository;
import com.alibaba.cloud.ai.example.manus2.service.AgentConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AgentConfigServiceImpl implements AgentConfigService {

    private final AgentConfigRepository agentConfigRepository;

    @Override
    @Transactional
    public AgentConfig createConfig(AgentConfig config) {
        validateConfig(config);
        return agentConfigRepository.save(config);
    }

    @Override
    @Transactional
    public AgentConfig updateConfig(String id, AgentConfig config) {
        AgentConfig existingConfig = agentConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent config not found"));

        // 只更新允许修改的字段
        existingConfig.setName(config.getName());
        existingConfig.setDescription(config.getDescription());
        existingConfig.setInstructions(config.getInstructions());
        existingConfig.setModel(config.getModel());
        existingConfig.setTools(config.getTools());
        existingConfig.setConnectedAgents(config.getConnectedAgents());
        existingConfig.setToolConfigs(config.getToolConfigs());
        existingConfig.setModelConfigs(config.getModelConfigs());
        existingConfig.setRagEnabled(config.isRagEnabled());
        existingConfig.setMaxCallsPerTurn(config.getMaxCallsPerTurn());
        existingConfig.setMaxTokensPerTurn(config.getMaxTokensPerTurn());
        existingConfig.setMaxTokensPerResponse(config.getMaxTokensPerResponse());
        existingConfig.setTemperature(config.getTemperature());
        existingConfig.setVisible(config.isVisible());
        existingConfig.setMetadata(config.getMetadata());

        validateConfig(existingConfig);
        return agentConfigRepository.save(existingConfig);
    }

    @Override
    @Transactional
    public void deleteConfig(String id) {
        agentConfigRepository.deleteById(id);
    }

    @Override
    public Optional<AgentConfig> findById(String id) {
        return agentConfigRepository.findById(id);
    }

    @Override
    public List<AgentConfig> findAllVisible() {
        return agentConfigRepository.findByVisibleTrue();
    }

    @Override
    public List<AgentConfig> findByName(String name) {
        return agentConfigRepository.findByNameContainingIgnoreCase(name);
    }

    @Override
    @Transactional
    public void updateVisibility(String id, boolean visible) {
        AgentConfig config = agentConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent config not found"));
        config.setVisible(visible);
        agentConfigRepository.save(config);
    }

    @Override
    @Transactional
    public void updateTools(String id, List<String> tools) {
        AgentConfig config = agentConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent config not found"));
        config.setTools(tools);
        agentConfigRepository.save(config);
    }

    @Override
    @Transactional
    public void updateModelConfig(String id, String model, Map<String, Object> config) {
        AgentConfig agentConfig = agentConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent config not found"));

        Map<String, Object> modelConfigs = agentConfig.getModelConfigs();
        if (modelConfigs == null) {
            modelConfigs = new HashMap<>();
        }
        modelConfigs.put(model, config);
        agentConfig.setModelConfigs(modelConfigs);

        agentConfigRepository.save(agentConfig);
    }

    @Override
    @Transactional
    public void updateToolConfig(String id, String tool, Map<String, Object> config) {
        AgentConfig agentConfig = agentConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent config not found"));

        Map<String, Object> toolConfigs = agentConfig.getToolConfigs();
        if (toolConfigs == null) {
            toolConfigs = new HashMap<>();
        }
        toolConfigs.put(tool, config);
        agentConfig.setToolConfigs(toolConfigs);

        agentConfigRepository.save(agentConfig);
    }

    @Override
    public void validateConfig(AgentConfig config) {
        if (config.getName() == null || config.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Agent name is required");
        }

        if (config.getModel() == null || config.getModel().trim().isEmpty()) {
            throw new IllegalArgumentException("Model is required");
        }

        if (config.getMaxCallsPerTurn() < 0) {
            throw new IllegalArgumentException("Max calls per turn must be non-negative");
        }

        if (config.getMaxTokensPerTurn() < 0) {
            throw new IllegalArgumentException("Max tokens per turn must be non-negative");
        }

        if (config.getMaxTokensPerResponse() < 0) {
            throw new IllegalArgumentException("Max tokens per response must be non-negative");
        }

        if (config.getTemperature() < 0 || config.getTemperature() > 2) {
            throw new IllegalArgumentException("Temperature must be between 0 and 2");
        }
    }
} 