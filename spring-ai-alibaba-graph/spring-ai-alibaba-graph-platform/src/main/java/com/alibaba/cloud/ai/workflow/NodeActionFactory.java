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
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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


    public NodeActionFactory(ChatModel chatModel) {

        // 注册默认工具
        ToolFactory.registerDefaultTools(chatModel);
    }

    /**
     * 根据Agent配置创建 NodeAction
     * 支持四种模式：llm、react、react_with_human、reflect
     */
    public NodeAction createNodeAction(WorkflowSchema.AgentConfig agentConfig, ChatModel chatModel) {
        String agentType = agentConfig.getType();

        switch (agentType.toLowerCase()) {
            case "llm":
                return createLLMAgent(agentConfig, chatModel);
            case "react":
                return createReactAgent(agentConfig, chatModel);
            case "react_with_human":
                return createReactAgentWithHuman(agentConfig, chatModel);
            case "reflect":
                return createReflectAgent(agentConfig, chatModel);
            default:
                throw new IllegalArgumentException("Unsupported agent type: " + agentType +
                        ". Supported types: llm, react, react_with_human, reflect");
        }
    }

    /**
     * 创建 LLM Agent
     */
    private NodeAction createLLMAgent(WorkflowSchema.AgentConfig agentConfig, ChatModel chatModel) {
        // 获取配置参数
        String prompt = agentConfig.getInstructions();
        List<ToolConfig> tools = agentConfig.getTools();

        // 如果工具列表为空，使用空列表
        if (tools == null) {
            tools = List.of();
        }
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        //转成toolcallback，过滤掉不存在的工具
        List<ToolCallback> toolCallbacks = tools.stream()
                .map(toolConfig -> {
                    String toolName = toolConfig.getName();
                    ToolCallback tool = ToolFactory.getTool(toolName);
                    if (tool == null && toolConfig.isAutoMock()) {
                        // 如果工具不存在且配置为自动mock，创建 MockToolCallback
                        return new MockToolCallback(chatClient, toolConfig);
                    }
                    return tool;
                })
                .filter(tool -> tool != null)
                .collect(Collectors.toList());

        try {
            // 获取输入输出键
            List<String> inputKeys = agentConfig.getInputKeys();
            if (inputKeys == null || inputKeys.isEmpty()) {
                inputKeys = List.of("input");
            }
            // LLM Agent只使用第一个inputKey
            String inputKey = inputKeys.get(0);
            String outputKey = agentConfig.getOutputKey() != null ? agentConfig.getOutputKey() : "output";

            // 创建 LlmNode
            LlmNode llmNode = LlmNode.builder()
                    .chatClient(chatClient)
                    .userPromptTemplate(inputKey)
                    .systemPromptTemplate(prompt)
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
    private NodeAction createReactAgent(WorkflowSchema.AgentConfig agentConfig, ChatModel chatModel) {
        // 获取配置参数
        String instruction = agentConfig.getInstructions();
        int maxIterations = 10;
        List<ToolConfig> tools = agentConfig.getTools();

        // 如果工具列表为空，抛出异常
        if (tools == null || tools.isEmpty()) {
            throw new RuntimeException("Agent " + agentConfig.getName() + " 没有配置工具");
        }
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                        .internalToolExecutionEnabled(false).build())
                .build();

        //转成toolcallback，过滤掉不存在的工具
        List<ToolCallback> toolCallbacks = tools.stream()
                .map(toolConfig -> {
                    String toolName = toolConfig.getName();
                    ToolCallback tool = ToolFactory.getTool(toolName);
                    if (tool == null && toolConfig.isAutoMock()) {
                        // 如果工具不存在且配置为自动mock，创建 MockToolCallback
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
                    .instruction(instruction)
                    .build();

            // 编译并返回 NodeAction
            reactAgent.getAndCompileGraph();
            
            // 获取inputKeys，如果为空则使用默认值
            List<String> inputKeys = agentConfig.getInputKeys();
            if (inputKeys == null || inputKeys.isEmpty()) {
                inputKeys = List.of("input");
            }
            
            return reactAgent.asNodeAction(
                    inputKeys,
                    agentConfig.getOutputKey() != null ? agentConfig.getOutputKey() : "output"
            );

        } catch (GraphStateException e) {
            throw new RuntimeException("Failed to create ReactAgent: " + e.getMessage(), e);
        }
    }

    /**
     * 创建 ReactAgentWithHuman
     */
    private NodeAction createReactAgentWithHuman(WorkflowSchema.AgentConfig agentConfig, ChatModel chatModel) {

        // 获取配置参数
        String prompt = agentConfig.getInstructions();
        int maxIterations = 10;
        List<ToolConfig> tools = agentConfig.getTools();

        // 如果工具列表为空，抛出异常
        if (tools == null || tools.isEmpty()) {
            throw new RuntimeException("Agent " + agentConfig.getName() + " 没有配置工具");
        }
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                .internalToolExecutionEnabled(false).build())
                .build();

        //转成toolcallback，过滤掉不存在的工具
        List<ToolCallback> toolCallbacks = tools.stream()
                .map(toolConfig -> {
                    String toolName = toolConfig.getName();
                    ToolCallback tool = ToolFactory.getTool(toolName);
                    if (tool == null && toolConfig.isAutoMock()) {
                        // 如果工具不存在且配置为自动mock，创建 MockToolCallback
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
            
            // 获取inputKeys，如果为空则使用默认值
            List<String> inputKeys = agentConfig.getInputKeys();
            if (inputKeys == null || inputKeys.isEmpty()) {
                inputKeys = List.of("input");
            }
            
            // ReactAgentWithHuman目前只支持单个inputKey，取第一个
            String inputKey = inputKeys.get(0);
            
            NodeActionWithConfig nodeActionWithConfig = reactAgentWithHuman.asNodeAction(
                    inputKey,
                    agentConfig.getOutputKey() != null ? agentConfig.getOutputKey() : "output"
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
    private NodeAction createReflectAgent(WorkflowSchema.AgentConfig agentConfig, ChatModel chatModel) {
        // 获取配置参数
        int maxIterations = 5;
        List<ToolConfig> tools = agentConfig.getTools();

        // 如果工具列表为空，抛出异常
        if (tools == null || tools.isEmpty()) {
            throw new RuntimeException("Agent " + agentConfig.getName() + " 没有配置工具");
        }
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                        .internalToolExecutionEnabled(false).build())
                .build();
        //转成toolcallback，过滤掉不存在的工具
        List<ToolCallback> toolCallbacks = tools.stream()
                .map(toolConfig -> {
                    String toolName = toolConfig.getName();
                    ToolCallback tool = ToolFactory.getTool(toolName);
                    if (tool == null && toolConfig.isAutoMock()) {
                        // 如果工具不存在且配置为自动mock，创建 MockToolCallback
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
                    List<String> inputKeys = agentConfig.getInputKeys();
                    if (inputKeys == null || inputKeys.isEmpty()) {
                        inputKeys = List.of("input");
                    }
                    // ReflectAgent只使用第一个inputKey
                    String inputKey = inputKeys.get(0);
                    String outputKey = agentConfig.getOutputKey() != null ? agentConfig.getOutputKey() : "output";

                    Object input = processInput(state, inputKey);
                    return Map.of(outputKey, "ReflectAgent processed: " + input);
                }
            };

        } catch (GraphStateException e) {
            throw new RuntimeException("Failed to create ReflectAgent: " + e.getMessage(), e);
        }
    }

    /**
     * 智能处理输入 - 参考 LangChain 的设计模式
     * 支持 String 和 Message 类型的输入
     */
    private Object processInput(OverAllState state, String inputKey) {
        Optional<Object> inputValue = state.value(inputKey);
        if (!inputValue.isPresent()) {
            return "";
        }

        Object input = inputValue.get();

        // 如果输入已经是 Message 类型，直接返回
        if (input instanceof org.springframework.ai.chat.messages.Message) {
            return input;
        }

        // 如果输入是 String 类型，转换为 UserMessage
        if (input instanceof String) {
            return new org.springframework.ai.chat.messages.UserMessage((String) input);
        }

        // 如果输入是 List<Message>，直接返回
        if (input instanceof List && !((List<?>) input).isEmpty() &&
                ((List<?>) input).get(0) instanceof org.springframework.ai.chat.messages.Message) {
            return input;
        }

        // 其他类型，转换为字符串后创建 UserMessage
        return new org.springframework.ai.chat.messages.UserMessage(input.toString());
    }

    /**
     * 创建图形动作（用于ReflectAgent）
     */
    private NodeAction createGraphAction(WorkflowSchema.AgentConfig agentConfig, List<ToolCallback> tools) {
        return new NodeAction() {
            @Override
            public Map<String, Object> apply(OverAllState state) throws Exception {
                List<String> inputKeys = agentConfig.getInputKeys();
                if (inputKeys == null || inputKeys.isEmpty()) {
                    inputKeys = List.of("input");
                }
                // 只使用第一个inputKey
                String inputKey = inputKeys.get(0);
                String outputKey = agentConfig.getOutputKey() != null ? agentConfig.getOutputKey() : "output";

                Object input = processInput(state, inputKey);
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
                List<String> inputKeys = agentConfig.getInputKeys();
                if (inputKeys == null || inputKeys.isEmpty()) {
                    inputKeys = List.of("input");
                }
                // 只使用第一个inputKey
                String inputKey = inputKeys.get(0);
                String outputKey = agentConfig.getOutputKey() != null ? agentConfig.getOutputKey() : "output";

                Object input = processInput(state, inputKey);
                return Map.of(outputKey, "Reflection on: " + input);
            }
        };
    }


} 