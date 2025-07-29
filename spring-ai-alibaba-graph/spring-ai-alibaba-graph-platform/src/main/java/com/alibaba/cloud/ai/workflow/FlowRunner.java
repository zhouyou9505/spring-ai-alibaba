package com.alibaba.cloud.ai.workflow;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
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

    private final ChatModel chatModel;
    private final NodeActionFactory nodeActionFactory;
    private final Map<String, CompiledGraph> compiledGraphs = new ConcurrentHashMap<>();
    private final Map<String, WorkflowSchema> workflowSchemas = new ConcurrentHashMap<>();

    public FlowRunner(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.nodeActionFactory = new NodeActionFactory(chatModel);
    }

    /**
     * 注册工作流
     */
    public void registerWorkflow(WorkflowSchema schema) throws GraphStateException {
        logger.info("注册工作流: {}", schema.getWorkflowId());

        // 存储工作流配置
        workflowSchemas.put(schema.getWorkflowId(), schema);

        // 构建编译图
        CompiledGraph compiledGraph = buildGraph(schema);
        compiledGraphs.put(schema.getWorkflowId(), compiledGraph);

        runWorkflow(schema.getWorkflowId(), Map.of("message","什么考试简单  可以抵税  计算机专业"));
    }

    /**
     * 运行工作流
     */
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

    /**
     * 构建图形化工作流
     */
    private CompiledGraph buildGraph(WorkflowSchema schema) throws GraphStateException {
        logger.info("构建工作流图形: {}", schema.getWorkflowId());

        // 创建状态工厂
        OverAllStateFactory stateFactory = createStateFactory(schema);

        // 创建图形构建器
        StateGraph graph = new StateGraph(stateFactory);

        // 添加Agent节点
        addAgents(graph, schema.getAgents());

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

            // 注册全局配置的键和策略
            if (schema.getGlobalConfig() != null) {
                for (String key : schema.getGlobalConfig().keySet()) {
                    state.registerKeyAndStrategy(key, new ReplaceStrategy());
                }
            }

            // 为所有Agent注册默认的键和策略
            if (schema.getAgents() != null) {
                for (WorkflowSchema.AgentConfig agent : schema.getAgents()) {
                    if (agent.getInputMapping() != null) {
                        for (Object value : agent.getInputMapping().values()) {
                            state.registerKeyAndStrategy(value.toString(), new ReplaceStrategy());
                        }
                    }
                    if (agent.getOutputMapping() != null) {
                        for (Object value : agent.getOutputMapping().values()) {
                            state.registerKeyAndStrategy(value.toString(), new ReplaceStrategy());
                        }
                    }
                }
            }

            return state;
        };
    }

    /**
     * 添加Agent到图形
     */
    private void addAgents(StateGraph graph, List<WorkflowSchema.AgentConfig> agents) {
        if (agents == null) {
            return;
        }

        for (WorkflowSchema.AgentConfig agentConfig : agents) {
            try {
                NodeAction nodeAction = nodeActionFactory.createNodeAction(agentConfig);
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
                    
                    // 创建复合条件边动作
                    AsyncEdgeAction compositeEdgeAction = createCompositeConditionEdgeAction(conditionalEdges);
                    
                    // 构建目标映射
                    Map<String, String> nextAgents = new HashMap<>();
                    for (WorkflowSchema.EdgeConfig edgeConfig : conditionalEdges) {
                        String toAgent = edgeConfig.getToAgentId();
                        String conditionKey = edgeConfig.getCondition();
                        
                        if (END.contains(toAgent)) {
                            nextAgents.put(conditionKey, END);
                        } else {
                            nextAgents.put(conditionKey, toAgent);
                        }
                    }
                    
                    // 添加默认分支（如果有普通边）
                    if (!normalEdges.isEmpty()) {
                        String defaultToAgent = normalEdges.get(0).getToAgentId();
                        if (END.contains(defaultToAgent)) {
                            nextAgents.put("default", END);
                        } else {
                            nextAgents.put("default", defaultToAgent);
                        }
                    }
                    
                    graph.addConditionalEdges(fromAgent, compositeEdgeAction, nextAgents);
                    logger.debug("添加复合条件边: {} -> {} (条件边数量: {})",
                            fromAgent, nextAgents, conditionalEdges.size());
                }
            }
        }
    }

    /**
     * 创建条件边动作
     */
    private AsyncEdgeAction createConditionEdgeAction(String condition) {
        return edge_async(state -> {
            try {
                // 解析 JSON 对象格式的条件
                // 格式: {"key": "value"}
                if (condition.startsWith("{") && condition.endsWith("}")) {
                    // 使用 JSON 解析
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
                            return actualValue.equals(expectedValue) ? "true" : "false";
                        }
                        return "false";
                    }
                }
                
            } catch (Exception e) {
                logger.warn("解析条件失败: {}", condition, e);
            }
            
            return "false"; // 默认返回 false
        });
    }

    /**
     * 创建复合条件边动作
     */
    private AsyncEdgeAction createCompositeConditionEdgeAction(List<WorkflowSchema.EdgeConfig> conditionalEdges) {
        return edge_async(state -> {
            // 按顺序评估每个条件
            for (WorkflowSchema.EdgeConfig edgeConfig : conditionalEdges) {
                String condition = edgeConfig.getCondition();
                if (evaluateCondition(condition, state)) {
                    // 如果条件为真，返回该条件的键
                    return condition;
                }
            }
            // 如果所有条件都为假，返回默认分支
            return "default";
        });
    }
    
    /**
     * 评估条件 - 支持 JSON 对象格式的条件
     */
    private boolean evaluateCondition(String condition, OverAllState state) {
        try {
            // 解析 JSON 对象格式的条件
            // 格式: {"key": "value"}
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
     * 评估简单条件
     */
    private boolean evaluateSimpleCondition(String condition, OverAllState state) {
        try {
            // 解析 JSON 对象格式的条件
            // 格式: {"key": "value"}
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