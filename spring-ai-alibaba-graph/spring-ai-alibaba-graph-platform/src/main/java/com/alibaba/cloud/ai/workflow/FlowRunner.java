package com.alibaba.cloud.ai.workflow;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;

/**
 * 工作流运行器，根据 WorkflowSchema 动态构建和运行工作流
 * 支持多Agent协作和复杂流程控制
 *
 * @author AI Assistant
 */
public class FlowRunner {

    private static final Logger logger = LoggerFactory.getLogger(FlowRunner.class);

    private final NodeActionFactory nodeActionFactory;
    private final Map<String, CompiledGraph> compiledGraphs = new ConcurrentHashMap<>();
    private final Map<String, WorkflowSchema> workflowSchemas = new ConcurrentHashMap<>();
    private ChatModel chatModel;
    public FlowRunner(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.nodeActionFactory = new NodeActionFactory(chatModel);
    }

    /**
     * 注册工作流
     */
    public void registerWorkflow(WorkflowSchema schema, ChatModel chatModel) throws GraphStateException {
        logger.info("注册工作流: {}", schema.getWorkflowId());

        // 存储工作流配置
        workflowSchemas.put(schema.getWorkflowId(), schema);

        // 构建编译图
        CompiledGraph compiledGraph = buildGraph(schema,chatModel);
        compiledGraphs.put(schema.getWorkflowId(), compiledGraph);

    }

    /**
     * 运行工作流
     */
    @SneakyThrows
    public Map<String, Object> runWorkflow(String workflowId, Map<String, Object> input) {
        logger.info("运行工作流: {}", workflowId);

        CompiledGraph compiledGraph = compiledGraphs.get(workflowId);
        if (compiledGraph == null) {
            throw new IllegalArgumentException("工作流未注册: " + workflowId);
        }

        Optional<OverAllState> result = compiledGraph.invoke(input);
        logger.info("工作流 {} 运行完成", workflowId);
        return result.get().data();
    }

    @SneakyThrows
    public Map<String, Object> runWorkflow(WorkflowSchema workflowSchema, Map<String, Object> input) {

        CompiledGraph compiledGraph = buildGraph(workflowSchema, chatModel);

        Optional<OverAllState> result = compiledGraph.invoke(input);
        return result.get().data();
    }


    /**
     * 构建图形化工作流
     */
    private CompiledGraph buildGraph(WorkflowSchema schema, ChatModel chatModel) throws GraphStateException {
        logger.info("构建工作流图形: {}", schema.getWorkflowId());

        // 创建状态工厂
        OverAllStateFactory stateFactory = createStateFactory(schema);

        // 创建图形构建器
        StateGraph graph = new StateGraph(stateFactory);

        // 添加Agent节点
        addAgents(graph, schema.getAgents(),chatModel);

        // 添加边
        addEdges(graph, schema.getEdges());

        // 编译图形
        CompiledGraph compiledGraph = graph.compile();

        // 打印图形表示（可选）
        GraphRepresentation representation = compiledGraph.getGraph(GraphRepresentation.Type.MERMAID);
        logger.info("工作流图形表示:\n{}", representation.content());

        return compiledGraph;
    }

    /**
     * 创建状态工厂
     */
    private OverAllStateFactory createStateFactory(WorkflowSchema schema) {
        return () -> {
            OverAllState state = new OverAllState();

            state.registerKeyAndStrategy("user_request",new AppendStrategy());

            // 注册全局配置的键和策略
            if (schema.getGlobalConfig() != null) {
                for (String key : schema.getGlobalConfig().keySet()) {
                    state.registerKeyAndStrategy(key, new ReplaceStrategy());
                }
            }

            // 为所有Agent注册默认的键和策略
            if (schema.getAgents() != null) {
                for (WorkflowSchema.AgentConfig agent : schema.getAgents()) {
                    // 注册 inputKey
                    if (agent.getInputKey() != null) {
                        state.registerKeyAndStrategy(agent.getInputKey(), new ReplaceStrategy());
                    }
                    // 注册 outputKey
                    if (agent.getOutputKey() != null) {
                        state.registerKeyAndStrategy(agent.getOutputKey(), new ReplaceStrategy());
                    }
                }
            }

            return state;
        };
    }

    /**
     * 添加Agent到图形
     */
    private void addAgents(StateGraph graph, List<WorkflowSchema.AgentConfig> agents, ChatModel chatModel) {
        if (agents == null) {
            return;
        }

        for (WorkflowSchema.AgentConfig agentConfig : agents) {
            try {
                NodeAction nodeAction = nodeActionFactory.createNodeAction(agentConfig, chatModel);
                AsyncNodeAction asyncNodeAction = node_async(nodeAction);

                graph.addNode(agentConfig.getAgentId(), asyncNodeAction);
                logger.debug("添加Agent: {} ({})", agentConfig.getAgentId(), agentConfig.getType());

            } catch (Exception e) {
                logger.error("创建Agent失败: {}", agentConfig.getAgentId(), e);
                throw new RuntimeException("创建Agent失败: " + agentConfig.getAgentId(), e);
            }
        }
    }

    /**
     * 添加边到图形
     */
    @SneakyThrows
    private void addEdges(StateGraph graph, List<WorkflowSchema.EdgeConfig> edges) {
        if (edges == null) {
            return;
        }

        // 按 fromAgentId 分组处理边
        Map<String, List<WorkflowSchema.EdgeConfig>> edgesByFromAgent = edges.stream()
            .collect(Collectors.groupingBy(WorkflowSchema.EdgeConfig::getFromAgentId));

        for (Map.Entry<String, List<WorkflowSchema.EdgeConfig>> entry : edgesByFromAgent.entrySet()) {
            String fromAgent = entry.getKey();
            List<WorkflowSchema.EdgeConfig> agentEdges = entry.getValue();

            if (START.contains(fromAgent)) {
                // 从开始节点出发的边，应该只有一个
                if (agentEdges.size() > 1) {
                    logger.warn("开始节点有多个边，只使用第一个: {}", fromAgent);
                }
                WorkflowSchema.EdgeConfig edgeConfig = agentEdges.get(0);
                String toAgent = edgeConfig.getToAgentId();
                String label = edgeConfig.getLabel();
                graph.addEdge(START, toAgent);
                logger.debug("添加开始边: {} -> {} ({})", fromAgent, toAgent, label);
            } else {
                // 处理普通Agent之间的边
                List<WorkflowSchema.EdgeConfig> conditionalEdges = agentEdges.stream()
                    .filter(edge -> edge.getCondition() != null)
                    .collect(Collectors.toList());
                
                List<WorkflowSchema.EdgeConfig> normalEdges = agentEdges.stream()
                    .filter(edge -> edge.getCondition() == null)
                    .collect(Collectors.toList());

                if (conditionalEdges.isEmpty()) {
                    // 只有普通边
                    for (WorkflowSchema.EdgeConfig edgeConfig : normalEdges) {
                        String toAgent = edgeConfig.getToAgentId();
                        String label = edgeConfig.getLabel();
                        WorkflowSchema.EdgeType edgeType = edgeConfig.getEdgeType();
                        
                        if (END.contains(toAgent)) {
                            graph.addEdge(fromAgent, END);
                            logger.debug("添加结束边: {} -> {} ({})", fromAgent, toAgent, label);
                        } else {
                            graph.addEdge(fromAgent, toAgent);
                            logger.debug("添加普通边: {} -> {} ({}, 类型: {})",
                                    fromAgent, toAgent, label, edgeType);
                        }
                    }
                } else {
                    // 有条件边，需要合并处理
                    if (normalEdges.size() > 0) {
                        logger.warn("Agent {} 同时有条件边和普通边，优先处理条件边", fromAgent);
                    }
                    
                    // 创建条件边动作
                    // 构建目标映射
                    Map<String, String> nextAgents = new HashMap<>();
                    
                    // 添加所有条件边到映射中
                    for (WorkflowSchema.EdgeConfig edgeConfig : conditionalEdges) {
                        String toAgent = edgeConfig.getToAgentId();
                        Map<String, Object> condition = edgeConfig.getCondition();
                        
                        // 将条件转换为字符串键，用于映射
                        String conditionKey = conditionToString(condition);
                        
                        if (END.contains(toAgent)) {
                            nextAgents.put(conditionKey, END);
                        } else {
                            nextAgents.put(conditionKey, toAgent);
                        }
                    }
                    
                    // 确保有默认分支
                    String defaultToAgent = null;
                    if (!normalEdges.isEmpty()) {
                        // 如果有普通边，使用第一个作为默认分支
                        defaultToAgent = normalEdges.get(0).getToAgentId();
                    } else if (!conditionalEdges.isEmpty()) {
                        // 如果没有普通边，使用第一个条件边的目标作为默认分支
                        defaultToAgent = conditionalEdges.get(0).getToAgentId();
                    } else {
                        // 如果没有任何边，使用 END
                        defaultToAgent = END;
                    }
                    
                    if (END.contains(defaultToAgent)) {
                        nextAgents.put("default", END);
                    } else {
                        nextAgents.put("default", defaultToAgent);
                    }
                    
                    logger.debug("添加条件边: {} -> {} (条件边数量: {}, 默认分支: {})",
                            fromAgent, nextAgents, conditionalEdges.size(), defaultToAgent);
                    
                    graph.addConditionalEdges(fromAgent,
                            edge_async(state -> {
                                logger.debug("评估条件边，条件数量: {}", conditionalEdges.size());

                                // 按照教程方式：从第一个条件边中获取条件键
                                if (!conditionalEdges.isEmpty()) {
                                    WorkflowSchema.EdgeConfig firstEdge = conditionalEdges.get(0);
                                    Map<String, Object> firstCondition = firstEdge.getCondition();
                                    
                                    if (firstCondition != null && !firstCondition.isEmpty()) {
                                        // 获取条件键（如 "request_category"）
                                        String conditionKey = firstCondition.keySet().iterator().next();
                                        logger.debug("检查状态键: {}", conditionKey);
                                        
                                        // 从状态中获取值
                                        Optional<Object> stateValue = state.value(conditionKey);
                                        if (stateValue.isPresent()) {
                                            return stateValue.get().toString();
                                        }
                                    }
                                }

                                logger.debug("没有找到匹配条件，返回默认分支");
                                // 如果没有找到匹配的条件，返回默认分支
                                return "default";
                            })
                            , nextAgents);
                }
            }
        }
    }


    /**
     * 评估条件 - 支持 Map 格式的条件
     */
    private boolean evaluateCondition(Map<String, Object> condition, OverAllState state) {
        try {
            logger.debug("开始评估条件: {}", condition);
            
            if (condition != null && !condition.isEmpty()) {
                // 获取第一个条件
                Map.Entry<String, Object> entry = condition.entrySet().iterator().next();
                String key = entry.getKey();
                String expectedValue = entry.getValue().toString();

                logger.debug("检查状态键: {}, 期望值: {}", key, expectedValue);

                // 从状态中获取值
                Optional<Object> stateValue = state.value(key);
                if (stateValue.isPresent()) {
                    String actualValue = stateValue.get().toString();
                    boolean result = actualValue.equals(expectedValue);
                    logger.debug("状态值: {}, 比较结果: {}", actualValue, result);
                    return result;
                } else {
                    logger.debug("状态中未找到键: {}", key);
                    return false;
                }
            }
        } catch (Exception e) {
            logger.warn("解析条件失败: {}", condition, e);
        }
        
        logger.debug("条件评估失败，返回 false");
        // 默认返回 false
        return false;
    }
    
    /**
     * 解析 JSON 字符串为 Map
     */
    private Map<String, String> parseJsonToMap(String json) {
        try {
            // 简单的 JSON 解析，处理 {"key": "value"} 格式
            String content = json.substring(1, json.length() - 1);
            Map<String, String> result = new HashMap<>();
            
            // 处理多个键值对的情况
            String[] pairs = content.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replace("\"", "").replace("'", "");
                    String value = keyValue[1].trim().replace("\"", "").replace("'", "");
                    result.put(key, value);
                }
            }
            
            return result;
        } catch (Exception e) {
            logger.warn("JSON 解析失败: {}", json, e);
            return new HashMap<>();
        }
    }

    /**
     * 评估简单条件 - 兼容 String 格式（向后兼容）
     */
    private boolean evaluateSimpleCondition(String condition, OverAllState state) {
        try {
            // 如果是 JSON 字符串格式，解析为 Map
            if (condition.startsWith("{") && condition.endsWith("}")) {
                Map<String, String> conditionMap = parseJsonToMap(condition);
                if (!conditionMap.isEmpty()) {
                    // 获取第一个条件
                    Map.Entry<String, String> entry = conditionMap.entrySet().iterator().next();
                    String key = entry.getKey();
                    String expectedValue = entry.getValue();
                    
                    // 从状态中获取值
                    Optional<Object> stateValue = state.value(key);
                    if (stateValue.isPresent()) {
                        String actualValue = stateValue.get().toString();
                        return actualValue.equals(expectedValue);
                    }
                    return false;
                }
            }
            
        } catch (Exception e) {
            logger.warn("解析条件失败: {}", condition, e);
        }
        
        return false;
    }

    /**
     * 将 Map 条件转换为字符串键
     */
    private String conditionToString(Map<String, Object> condition) {
        if (condition == null || condition.isEmpty()) {
            return "default";
        }
        
        // 获取第一个键值对，格式化为 "key:value"
        Map.Entry<String, Object> entry = condition.entrySet().iterator().next();
        String key = entry.getKey();
        String value = entry.getValue().toString();
        
        // 返回简洁的格式，如 "math", "translation", "data_analysis"
        return value;
    }

    /**
     * 获取已注册的工作流ID列表
     */
    public List<String> getRegisteredWorkflowIds() {
        return List.copyOf(compiledGraphs.keySet());
    }

    /**
     * 检查工作流是否已注册
     */
    public boolean isWorkflowRegistered(String workflowId) {
        return compiledGraphs.containsKey(workflowId);
    }

    /**
     * 移除工作流
     */
    public void removeWorkflow(String workflowId) {
        CompiledGraph removed = compiledGraphs.remove(workflowId);
        WorkflowSchema schemaRemoved = workflowSchemas.remove(workflowId);
        if (removed != null) {
            logger.info("移除工作流: {}", workflowId);
        }
    }

    /**
     * 获取工作流配置
     */
    public WorkflowSchema getWorkflowSchema(String workflowId) {
        return workflowSchemas.get(workflowId);
    }

    /**
     * 获取工作流统计信息
     */
    public Map<String, Object> getWorkflowStats(String workflowId) {
        Map<String, Object> stats = new HashMap<>();
        
        WorkflowSchema schema = workflowSchemas.get(workflowId);
        if (schema != null) {
            stats.put("workflowId", workflowId);
            stats.put("agentCount", schema.getAgents() != null ? schema.getAgents().size() : 0);
            stats.put("edgeCount", schema.getEdges() != null ? schema.getEdges().size() : 0);
            
            // 统计工具数量
            int totalTools = 0;
            if (schema.getAgents() != null) {
                for (WorkflowSchema.AgentConfig agent : schema.getAgents()) {
                    if (agent.getTools() != null) {
                        totalTools += agent.getTools().size();
                    }
                }
            }
            stats.put("toolCount", totalTools);
        }
        
        return stats;
    }
    
    /**
     * 获取工具工厂
     */
    public com.alibaba.cloud.ai.service.ToolFactory getToolFactory() {
        return com.alibaba.cloud.ai.service.ToolFactory.getInstance();
    }
} 