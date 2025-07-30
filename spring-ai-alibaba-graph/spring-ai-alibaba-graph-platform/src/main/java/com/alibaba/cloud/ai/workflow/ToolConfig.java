package com.alibaba.cloud.ai.workflow;

import lombok.Data;

import java.util.Map;

/**
 * 工具配置类
 * 简化的工具配置，只包含常用参数
 * 
 * @author AI Assistant
 */
@Data
public class ToolConfig {

    public String name;
    public String description;
    public Map<String, Object> parameters;
    public boolean autoMock;

    public ToolConfig() {
    }

    public ToolConfig(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public ToolConfig(String name, String description, Map<String, Object> parameters) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
    }

    /**
     * 从 Map 创建 ToolConfig
     */
    public static ToolConfig fromMap(Map<String, Object> toolMap) {
        ToolConfig config = new ToolConfig();
        
        config.setName((String) toolMap.get("name"));
        config.setDescription((String) toolMap.get("description"));
        config.setParameters((Map<String, Object>) toolMap.get("parameters"));
        config.setAutoMock((Boolean) toolMap.get("autoMock"));
        return config;
    }

    /**
     * 转换为 Map
     */
    public Map<String, Object> toMap() {
        return Map.of(
            "name", name != null ? name : "",
            "description", description != null ? description : "",
            "parameters", parameters != null ? parameters : Map.of(),
            "autoMock", autoMock
        );
    }

    /**
     * 检查是否为自动mock工具
     */
    public boolean isAutoMock() {
        return autoMock;
    }
}
