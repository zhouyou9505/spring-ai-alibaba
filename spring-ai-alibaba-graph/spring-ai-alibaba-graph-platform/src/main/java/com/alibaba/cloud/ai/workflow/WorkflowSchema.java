package com.alibaba.cloud.ai.workflow;

import lombok.Data;

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

    // Getters and Setters
    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<AgentConfig> getAgents() {
        return agents;
    }

    public void setAgents(List<AgentConfig> agents) {
        this.agents = agents;
    }

    public List<EdgeConfig> getEdges() {
        return edges;
    }

    public void setEdges(List<EdgeConfig> edges) {
        this.edges = edges;
    }

    public Map<String, Object> getGlobalConfig() {
        return globalConfig;
    }

    public void setGlobalConfig(Map<String, Object> globalConfig) {
        this.globalConfig = globalConfig;
    }

    public WorkflowMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(WorkflowMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Agent配置 - 支持四种 Agent 模式：llm、react、react_with_human、reflect
     */
    @Data
    public static class AgentConfig {
        private String agentId;
        private String name;
        private String type; // "llm", "react", "react_with_human", "reflect"
        private String description;
        private String instructions; // Agent的具体指令
        private String model; // 使用的模型
        private Map<String, Object> config;
        private String inputKey; // 输入键名
        private String outputKey; // 输出键名
        private AgentOptions options;
        private List<ToolConfig> tools; // 多Agent协作工具

        public AgentConfig() {
        }

        public AgentConfig(String agentId, String name, String type, String description,
                           String instructions, String model, Map<String, Object> config,
                           String inputKey, String outputKey,
                           AgentOptions options, List<ToolConfig> tools) {
            this.agentId = agentId;
            this.name = name;
            this.type = type;
            this.description = description;
            this.instructions = instructions;
            this.model = model;
            this.config = config;
            this.inputKey = inputKey;
            this.outputKey = outputKey;
            this.options = options;
            this.tools = tools;
        }

        // Getters and Setters for inputKey and outputKey
        public String getInputKey() {
            return inputKey;
        }

        public void setInputKey(String inputKey) {
            this.inputKey = inputKey;
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

        // Getters and Setters
        public boolean isToggleAble() {
            return toggleAble;
        }

        public void setToggleAble(boolean toggleAble) {
            this.toggleAble = toggleAble;
        }

        public String getRagReturnType() {
            return ragReturnType;
        }

        public void setRagReturnType(String ragReturnType) {
            this.ragReturnType = ragReturnType;
        }

        public int getRagK() {
            return ragK;
        }

        public void setRagK(int ragK) {
            this.ragK = ragK;
        }

        public String getControlType() {
            return controlType;
        }

        public void setControlType(String controlType) {
            this.controlType = controlType;
        }

        public String getOutputVisibility() {
            return outputVisibility;
        }

        public void setOutputVisibility(String outputVisibility) {
            this.outputVisibility = outputVisibility;
        }

        public String getExamples() {
            return examples;
        }

        public void setExamples(String examples) {
            this.examples = examples;
        }

        public Integer getOrder() {
            return order;
        }

        public void setOrder(Integer order) {
            this.order = order;
        }

        public boolean isDisabled() {
            return disabled;
        }

        public void setDisabled(boolean disabled) {
            this.disabled = disabled;
        }

        public boolean isLocked() {
            return locked;
        }

        public void setLocked(boolean locked) {
            this.locked = locked;
        }

        public Integer getMaxCallsPerParentAgent() {
            return maxCallsPerParentAgent;
        }

        public void setMaxCallsPerParentAgent(Integer maxCallsPerParentAgent) {
            this.maxCallsPerParentAgent = maxCallsPerParentAgent;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }

    /**
     * 边配置 - 支持复杂条件分支和循环
     */
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

        // Getters and Setters
        public String getEdgeId() {
            return edgeId;
        }

        public void setEdgeId(String edgeId) {
            this.edgeId = edgeId;
        }

        public String getFromAgentId() {
            return fromAgentId;
        }

        public void setFromAgentId(String fromAgentId) {
            this.fromAgentId = fromAgentId;
        }

        public String getToAgentId() {
            return toAgentId;
        }

        public void setToAgentId(String toAgentId) {
            this.toAgentId = toAgentId;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Map<String, Object> getCondition() {
            return condition;
        }

        public void setCondition(Map<String, Object> condition) {
            this.condition = condition;
        }

        public EdgeType getEdgeType() {
            return edgeType;
        }

        public void setEdgeType(EdgeType edgeType) {
            this.edgeType = edgeType;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public void setConfig(Map<String, Object> config) {
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