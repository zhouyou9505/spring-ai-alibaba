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


        logger.info("工作流 {} 注册成功，包含 {} 个Agent", schema.getWorkflowId(),
                schema.getAgents() != null ? schema.getAgents().size() : 0);
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
        GraphRepresentation representation = compiledGraph.getGraph(GraphRepresentation.Type.PLANTUML);
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

        for (WorkflowSchema.EdgeConfig edgeConfig : edges) {
            String fromAgent = edgeConfig.getFromAgentId();
            String toAgent = edgeConfig.getToAgentId();
            String label = edgeConfig.getLabel();
            WorkflowSchema.EdgeType edgeType = edgeConfig.getEdgeType();

            if (START.contains(fromAgent)) {
                // 从开始节点出发的边
                graph.addEdge(START, toAgent);
                logger.debug("添加开始边: {} -> {} ({})", fromAgent, toAgent, label);
            } else if (END.contains(toAgent)) {
                // 到结束节点的边
                if (edgeConfig.getCondition() != null) {
                    // 条件边
                    AsyncEdgeAction edgeAction = createConditionEdgeAction(edgeConfig.getCondition());
                    graph.addConditionalEdges(fromAgent, edgeAction,
                            Map.of("true", END, "false", END));
                } else {
                    // 普通边
                    graph.addEdge(fromAgent, END);
                }
                logger.debug("添加结束边: {} -> {} ({})", fromAgent, toAgent, label);
            } else {
                // 普通Agent之间的边
                if (edgeConfig.getCondition() != null) {
                    // 条件边
                    AsyncEdgeAction edgeAction = createConditionEdgeAction(edgeConfig.getCondition());
                    Map<String, String> nextAgents = new HashMap<>();
                    nextAgents.put("true", toAgent);
                    nextAgents.put("false", toAgent); // 可以根据需要设置不同的false分支

                    graph.addConditionalEdges(fromAgent, edgeAction, nextAgents);
                    logger.debug("添加条件边: {} -> {} (条件: {}, 类型: {})",
                            fromAgent, toAgent, edgeConfig.getCondition(), edgeType);
                } else {
                    // 普通边
                    graph.addEdge(fromAgent, toAgent);
                    logger.debug("添加普通边: {} -> {} ({}, 类型: {})",
                            fromAgent, toAgent, label, edgeType);
                }
            }
        }
    }

    /**
     * 创建条件边动作
     */
    private AsyncEdgeAction createConditionEdgeAction(String condition) {
        return edge_async(state -> {
            // 支持更复杂的条件评估
            if (condition.contains("==")) {
                String[] parts = condition.split("==");
                String key = parts[0].trim();
                String value = parts[1].trim().replace("\"", "");

                boolean result = state.value(key)
                        .map(val -> val.toString().equals(value))
                        .orElse(false);

                return result ? "true" : "false";
            } else if (condition.contains("!=")) {
                String[] parts = condition.split("!=");
                String key = parts[0].trim();
                String value = parts[1].trim().replace("\"", "");

                boolean result = state.value(key)
                        .map(val -> !val.toString().equals(value))
                        .orElse(false);

                return result ? "true" : "false";
            } else if (condition.contains(">=")) {
                String[] parts = condition.split(">=");
                String key = parts[0].trim();
                String value = parts[1].trim();

                boolean result = state.value(key)
                        .map(val -> {
                            try {
                                double valNum = Double.parseDouble(val.toString());
                                double compareNum = Double.parseDouble(value);
                                return valNum >= compareNum;
                            } catch (NumberFormatException e) {
                                return false;
                            }
                        })
                        .orElse(false);

                return result ? "true" : "false";
            } else if (condition.contains(">")) {
                String[] parts = condition.split(">");
                String key = parts[0].trim();
                String value = parts[1].trim();

                boolean result = state.value(key)
                        .map(val -> {
                            try {
                                double valNum = Double.parseDouble(val.toString());
                                double compareNum = Double.parseDouble(value);
                                return valNum > compareNum;
                            } catch (NumberFormatException e) {
                                return false;
                            }
                        })
                        .orElse(false);

                return result ? "true" : "false";
            } else if (condition.contains("&&")) {
                String[] parts = condition.split("&&");
                boolean result = true;
                for (String part : parts) {
                    result = result && evaluateSimpleCondition(part.trim(), state);
                }
                return result ? "true" : "false";
            } else if (condition.contains("||")) {
                String[] parts = condition.split("\\|\\|");
                boolean result = false;
                for (String part : parts) {
                    result = result || evaluateSimpleCondition(part.trim(), state);
                }
                return result ? "true" : "false";
            }

            return "true"; // 默认返回true
        });
    }

    /**
     * 评估简单条件
     */
    private boolean evaluateSimpleCondition(String condition, OverAllState state) {
        if (condition.contains("==")) {
            String[] parts = condition.split("==");
            String key = parts[0].trim();
            String value = parts[1].trim().replace("\"", "");
            return state.value(key).map(val -> val.toString().equals(value)).orElse(false);
        } else if (condition.contains("!=")) {
            String[] parts = condition.split("!=");
            String key = parts[0].trim();
            String value = parts[1].trim().replace("\"", "");
            return state.value(key).map(val -> !val.toString().equals(value)).orElse(false);
        }
        return true;
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