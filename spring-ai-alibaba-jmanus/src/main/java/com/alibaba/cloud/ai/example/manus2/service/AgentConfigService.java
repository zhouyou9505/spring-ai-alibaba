package com.alibaba.cloud.ai.example.manus2.service;

import com.alibaba.cloud.ai.example.manus2.model.AgentConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AgentConfigService {
    /**
     * 创建代理配置
     */
    AgentConfig createConfig(AgentConfig config);

    /**
     * 更新代理配置
     */
    AgentConfig updateConfig(String id, AgentConfig config);

    /**
     * 删除代理配置
     */
    void deleteConfig(String id);

    /**
     * 根据ID查找代理配置
     */
    Optional<AgentConfig> findById(String id);

    /**
     * 查找所有可见的代理配置
     */
    List<AgentConfig> findAllVisible();

    /**
     * 根据名称查找代理配置
     */
    List<AgentConfig> findByName(String name);

    /**
     * 更新代理配置的可见性
     */
    void updateVisibility(String id, boolean visible);

    /**
     * 更新代理配置的工具列表
     */
    void updateTools(String id, List<String> tools);

    /**
     * 更新代理配置的模型配置
     */
    void updateModelConfig(String id, String model, Map<String, Object> config);

    /**
     * 更新代理配置的工具配置
     */
    void updateToolConfig(String id, String tool, Map<String, Object> config);

    /**
     * 验证代理配置
     */
    void validateConfig(AgentConfig config);
} 