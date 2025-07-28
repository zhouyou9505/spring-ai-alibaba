package com.alibaba.cloud.ai.workflow;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.ReactAgentWithHuman;
import com.alibaba.cloud.ai.graph.agent.ReflectAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.service.MockToolCallback;
import com.alibaba.cloud.ai.service.ToolFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * NodeAction 工厂类，根据配置动态创建 NodeAction 实例
 * 支持四种 Agent 模式：llm、react、react_with_human、reflect
 * 使用工具工厂来管理工具
 * 
 * @author AI Assistant
 */
@Slf4j
public class NodeActionFactory {
    
    private final ChatModel chatModel;
    private final ChatClient chatClient;
    
    public NodeActionFactory(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.chatClient = ChatClient.builder(chatModel).build();
        
        // 注册默认工具
        ToolFactory.registerDefaultTools(chatClient);
    }
    
    /**
     * 根据Agent配置创建 NodeAction
     * 支持四种模式：llm、react、react_with_human、reflect
     */
    public NodeAction createNodeAction(WorkflowSchema.AgentConfig agentConfig) {
        String agentType = agentConfig.getType();
        
        switch (agentType.toLowerCase()) {
            case "llm":
                return createLLMAgent(agentConfig);
            case "react":
                return createReactAgent(agentConfig);
            case "react_with_human":
                return createReactAgentWithHuman(agentConfig);
            case "reflect":
                return createReflectAgent(agentConfig);
            default:
                throw new IllegalArgumentException("Unsupported agent type: " + agentType + 
                    ". Supported types: llm, react, react_with_human, reflect");
        }
    }

    /**
     * 创建 LLM Agent
     */
    private NodeAction createLLMAgent(WorkflowSchema.AgentConfig agentConfig) {
        // 获取配置参数
        String prompt = agentConfig.getInstructions();
        List<ToolConfig> tools = agentConfig.getTools();
        
        //转成toolcallback，过滤掉不存在的工具
        List<ToolCallback> toolCallbacks = tools.stream()
            .map(toolConfig -> {
                String toolName = toolConfig.getName();
                ToolCallback tool = ToolFactory.getTool(toolName);
                if (tool == null) {
                    // 如果工具不存在，创建 MockToolCallback
                    return new MockToolCallback(chatClient, toolConfig);
                }
                return tool;
            })
            .filter(tool -> tool != null)
            .collect(Collectors.toList());
            
        try {
            // 获取输入输出键
            String inputKey = (String) agentConfig.getConfig().getOrDefault("inputKey", "input");
            String outputKey = (String) agentConfig.getConfig().getOrDefault("outputKey", "output");
            
            // 创建 LlmNode
            LlmNode llmNode = LlmNode.builder()
                .chatClient(chatClient)
                .systemPromptTemplate(prompt)
                .userPromptTemplate("请处理以下输入：{" + inputKey + "}")
                .messagesKey("messages")
                .outputKey(outputKey)
                .toolCallbacks(toolCallbacks)
                .build();
            
            // 直接返回 LlmNode 作为 NodeAction
            return llmNode;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create LLMAgent: " + e.getMessage(), e);
        }
    }

    /**
     * 创建 ReactAgent
     */
    private NodeAction createReactAgent(WorkflowSchema.AgentConfig agentConfig) {
        // 获取配置参数
        String prompt = agentConfig.getInstructions();
        int maxIterations = 10;
        List<ToolConfig> tools = agentConfig.getTools();
        
        // 如果工具列表为空，自动添加 mock_tool
        if (tools == null || tools.isEmpty()) {
//            tools = List.of(createMockToolConfig("mock_tool"));
//            log.info("Agent {} 没有配置工具，自动添加 mock_tool", agentConfig.getName());
            throw new RuntimeException("Agent " + agentConfig.getName() + " 没有配置工具");
        }
        
        //转成toolcallback，过滤掉不存在的工具
        List<ToolCallback> toolCallbacks = tools.stream()
            .map(toolConfig -> {
                String toolName = toolConfig.getName();
                ToolCallback tool = ToolFactory.getTool(toolName);
                if (tool == null) {
                    // 如果工具不存在，创建 MockToolCallback
                    return new MockToolCallback(chatClient, toolConfig);
                }
                return tool;
            })
            .filter(tool -> tool != null)
            .collect(Collectors.toList());
            
        try {
            ReactAgent reactAgent = ReactAgent.builder()
                .name(agentConfig.getName())
                .chatClient(chatClient)
                .tools(toolCallbacks)
                .maxIterations(maxIterations)
                .build();
            
            // 编译并返回 NodeAction
            reactAgent.getAndCompileGraph();
            return reactAgent.asNodeAction(
                (String) agentConfig.getConfig().getOrDefault("inputKey", "input"),
                (String) agentConfig.getConfig().getOrDefault("outputKey", "output")
            );
            
        } catch (GraphStateException e) {
            throw new RuntimeException("Failed to create ReactAgent: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建 ReactAgentWithHuman
     */
    private NodeAction createReactAgentWithHuman(WorkflowSchema.AgentConfig agentConfig) {

        // 获取配置参数
        String prompt = agentConfig.getInstructions();
        int maxIterations = 10;
        List<ToolConfig> tools = agentConfig.getTools();
        
        // 如果工具列表为空，自动添加 mock_tool
        if (tools == null || tools.isEmpty()) {
//            tools = List.of(createMockToolConfig("mock_tool"));
//            log.info("Agent {} 没有配置工具，自动添加 mock_tool", agentConfig.getName());
            throw new RuntimeException("Agent " + agentConfig.getName() + " 没有配置工具");

        }
        
        //转成toolcallback，过滤掉不存在的工具
        List<ToolCallback> toolCallbacks = tools.stream()
            .map(toolConfig -> {
                String toolName = toolConfig.getName();
                ToolCallback tool = ToolFactory.getTool(toolName);
                if (tool == null) {
                    // 如果工具不存在，创建 MockToolCallback
                    return new MockToolCallback(chatClient, toolConfig);
                }
                return tool;
            })
            .filter(tool -> tool != null)
            .collect(Collectors.toList());
            
        try {

            ReactAgentWithHuman reactAgentWithHuman;
            reactAgentWithHuman = ReactAgentWithHuman.builder()
                    .chatClient(chatClient)
                    .prompt(prompt)
                    .tools(toolCallbacks)
                    .maxIterations(maxIterations)
                    .build();

            // 编译并返回 NodeAction
            reactAgentWithHuman.getAndCompileGraph();
            NodeActionWithConfig nodeActionWithConfig = reactAgentWithHuman.asNodeAction(
                (String) agentConfig.getConfig().getOrDefault("inputKey", "input"),
                (String) agentConfig.getConfig().getOrDefault("outputKey", "output")
            );
            
            // 将 NodeActionWithConfig 适配为 NodeAction
            return new NodeAction() {
                @Override
                public Map<String, Object> apply(OverAllState state) throws Exception {
                    return nodeActionWithConfig.apply(state, null);
                }
            };
            
        } catch (GraphStateException e) {
            throw new RuntimeException("Failed to create ReactAgentWithHuman: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建 ReflectAgent
     */
    private NodeAction createReflectAgent(WorkflowSchema.AgentConfig agentConfig) {
        // 获取配置参数
        int maxIterations = 5;
        List<ToolConfig> tools = agentConfig.getTools();
        
        // 如果工具列表为空，自动添加 mock_tool
        if (tools == null || tools.isEmpty()) {
//            tools = List.of(createMockToolConfig("mock_tool"));
//            log.info("Agent {} 没有配置工具，自动添加 mock_tool", agentConfig.getName());
            throw new RuntimeException("Agent " + agentConfig.getName() + " 没有配置工具");

        }
        
        //转成toolcallback，过滤掉不存在的工具
        List<ToolCallback> toolCallbacks = tools.stream()
            .map(toolConfig -> {
                String toolName = toolConfig.getName();
                ToolCallback tool = ToolFactory.getTool(toolName);
                if (tool == null) {
                    // 如果工具不存在，创建 MockToolCallback
                    return new MockToolCallback(chatClient, toolConfig);
                }
                return tool;
            })
            .filter(tool -> tool != null)
            .collect(Collectors.toList());
        
        try {
            // 创建图形动作和反思动作
            NodeAction graphAction = createGraphAction(agentConfig, toolCallbacks);
            NodeAction reflectionAction = createReflectionAction(agentConfig);
            
            ReflectAgent reflectAgent = ReflectAgent.builder()
                .name(agentConfig.getName())
                .graph(graphAction)
                .reflection(reflectionAction)
                .maxIterations(maxIterations)
                .build();
            
            reflectAgent.getAndCompileGraph();
            // ReflectAgent 没有 asNodeAction 方法，直接返回编译后的图形
            return new NodeAction() {
                @Override
                public Map<String, Object> apply(OverAllState state) throws Exception {
                    // 这里需要根据实际需求实现
                    // 暂时返回一个简单的实现
                    String inputKey = (String) agentConfig.getConfig().getOrDefault("inputKey", "input");
                    String outputKey = (String) agentConfig.getConfig().getOrDefault("outputKey", "output");
                    
                    Object input = state.value(inputKey).orElse("");
                    return Map.of(outputKey, "ReflectAgent processed: " + input);
                }
            };
            
        } catch (GraphStateException e) {
            throw new RuntimeException("Failed to create ReflectAgent: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建图形动作（用于ReflectAgent）
     */
    private NodeAction createGraphAction(WorkflowSchema.AgentConfig agentConfig, List<ToolCallback> tools) {
        return new NodeAction() {
            @Override
            public Map<String, Object> apply(OverAllState state) throws Exception {
                String inputKey = (String) agentConfig.getConfig().getOrDefault("inputKey", "input");
                String outputKey = (String) agentConfig.getConfig().getOrDefault("outputKey", "output");
                
                Object input = state.value(inputKey).orElse("");
                // 这里可以添加工具调用逻辑
                return Map.of(outputKey, "Graph processed: " + input);
            }
        };
    }
    
    /**
     * 创建反思动作（用于ReflectAgent）
     */
    private NodeAction createReflectionAction(WorkflowSchema.AgentConfig agentConfig) {
        return new NodeAction() {
            @Override
            public Map<String, Object> apply(OverAllState state) throws Exception {
                String inputKey = (String) agentConfig.getConfig().getOrDefault("inputKey", "input");
                String outputKey = (String) agentConfig.getConfig().getOrDefault("outputKey", "output");
                
                Object input = state.value(inputKey).orElse("");
                return Map.of(outputKey, "Reflection on: " + input);
            }
        };
    }
    
    /**
     * 创建 MockTool 配置
     */
    private ToolConfig createMockToolConfig(String toolName) {
        Map<String, Object> mockParameters = Map.of(
            "type", "object",
            "properties", Map.of(
                "input", Map.of(
                    "type", "string",
                    "description", "输入参数"
                ),
                "mockType", Map.of(
                    "type", "string",
                    "description", "模拟类型",
                    "enum", List.of("success", "error", "random")
                )
            ),
            "required", List.of("input")
        );
        
        return new ToolConfig(
            toolName,
            "这是一个模拟工具，用于测试和演示。当实际工具不可用时，提供模拟响应。",
            mockParameters
        );
    }
} 