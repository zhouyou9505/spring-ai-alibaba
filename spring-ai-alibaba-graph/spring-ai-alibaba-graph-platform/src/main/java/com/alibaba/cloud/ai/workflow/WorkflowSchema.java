package com.alibaba.cloud.ai.workflow;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 工作流配置结构 - 支持多Agent协作和复杂流程
 *
 * @author AI Assistant
 */
@Data
public class WorkflowSchema {

    private String workflowId;
    private String name;
    private String description;
    private String version;
    private List<AgentConfig> agents;
    private List<EdgeConfig> edges;
    private Map<String, Object> globalConfig;
    private WorkflowMetadata metadata;

    public WorkflowSchema() {
    }


    public WorkflowSchema(String workflowId, String name, String description,
                          List<AgentConfig> agents, List<EdgeConfig> edges,
                          Map<String, Object> globalConfig) {
        this.workflowId = workflowId;
        this.name = name;
        this.description = description;
        this.agents = agents;
        this.edges = edges;
        this.globalConfig = globalConfig;
    }

    /**
     * Agent配置 - 支持四种 Agent 模式：llm、react
     */
    @Data
    public static class AgentConfig {
        private String agentId;
        private String name;
        private String type; // "llm", "react"
        private String description;
        private String instructions; // Agent的具体指令
        private String model; // 使用的模型
        private Map<String, Object> config;
        private List<String> inputKeys; // 输入键名列表，支持多个inputKey
        private String outputKey; // 输出键名
        private AgentOptions options;
        private List<ToolConfig> tools; // 多Agent协作工具

        public AgentConfig() {
        }

        public AgentConfig(String agentId, String name, String type, String description,
                           String instructions, String model, Map<String, Object> config,
                           List<String> inputKeys, String outputKey,
                           AgentOptions options, List<ToolConfig> tools) {
            this.agentId = agentId;
            this.name = name;
            this.type = type;
            this.description = description;
            this.instructions = instructions;
            this.model = model;
            this.config = config;
            this.inputKeys = inputKeys;
            this.outputKey = outputKey;
            this.options = options;
            this.tools = tools;
        }

        // Getters and Setters for inputKeys and outputKey
        public List<String> getInputKeys() {
            return inputKeys;
        }

        public void setInputKeys(List<String> inputKeys) {
            this.inputKeys = inputKeys;
        }

        // 为了向后兼容，保留单个inputKey的getter/setter
        public String getInputKey() {
            if (inputKeys != null && !inputKeys.isEmpty()) {
                return inputKeys.get(0);
            }
            return null;
        }

        public void setInputKey(String inputKey) {
            if (inputKey != null) {
                this.inputKeys = new ArrayList<>();
                this.inputKeys.add(inputKey);
            } else {
                this.inputKeys = null;
            }
        }

        public String getOutputKey() {
            return outputKey;
        }

        public void setOutputKey(String outputKey) {
            this.outputKey = outputKey;
        }
    }

    /**
     * Agent选项配置
     */
    @Data
    public static class AgentOptions {
        private boolean toggleAble = true;
        private String ragReturnType;
        private int ragK = 5;
        private String controlType = "auto";
        private String outputVisibility = "public";
        private String examples;
        private Integer order;
        private boolean disabled = false;
        private boolean locked = false;
        private Integer maxCallsPerParentAgent;
        private int maxRetries = 3;
        private long timeout = 30000;

        public AgentOptions() {
        }

    }

    /**
     * 边配置 - 支持复杂条件分支和循环
     */
    @Data
    public static class EdgeConfig {
        private String edgeId;
        private String fromAgentId;
        private String toAgentId;
        private String label; // 边的标签，如"主题"、"是否通过?"
        private Map<String, Object> condition; // 条件表达式，使用JSON对象格式
        private EdgeType edgeType; // 边的类型
        private Map<String, Object> config;

        public EdgeConfig() {
        }

        public EdgeConfig(String edgeId, String fromAgentId, String toAgentId,
                          String label, Map<String, Object> condition, EdgeType edgeType,
                          Map<String, Object> config) {
            this.edgeId = edgeId;
            this.fromAgentId = fromAgentId;
            this.toAgentId = toAgentId;
            this.label = label;
            this.condition = condition;
            this.edgeType = edgeType;
            this.config = config;
        }


    }

    /**
     * 边类型枚举
     */
    public enum EdgeType {
        SEQUENTIAL,    // 顺序执行
        CONDITIONAL,   // 条件分支
        LOOP,          // 循环
        PARALLEL       // 并行执行
    }

    /**
     * 工作流元数据
     */
    @Data
    public static class WorkflowMetadata {
        private String createdAt;
        private String lastUpdatedAt;
        private String author;
        private List<String> tags;
        private String category;
        private Map<String, Object> customFields;

        public WorkflowMetadata() {
        }

        // Getters and Setters
        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getLastUpdatedAt() {
            return lastUpdatedAt;
        }

        public void setLastUpdatedAt(String lastUpdatedAt) {
            this.lastUpdatedAt = lastUpdatedAt;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public Map<String, Object> getCustomFields() {
            return customFields;
        }

        public void setCustomFields(Map<String, Object> customFields) {
            this.customFields = customFields;
        }
    }

} 