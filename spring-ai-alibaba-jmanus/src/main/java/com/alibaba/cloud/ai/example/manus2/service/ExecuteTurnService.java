package com.alibaba.cloud.ai.example.manus2.service;

import com.alibaba.cloud.ai.example.manus2.tools.WebSearchTool;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateFactory;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.alibaba.cloud.ai.example.manus2.model.*;
import com.alibaba.cloud.ai.example.manus2.model.enums.OutputVisibility;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import org.springframework.ai.chat.messages.Message;
import com.alibaba.cloud.ai.graph.agent.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExecuteTurnService {

    private ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final ToolService toolService;
    private final MongoTemplate mongoTemplate;
    private final RagTool ragTool;
    private final WebSearchTool webSearchTool;

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/rowboat}")
    private String mongoUri;

    private static final int DEFAULT_MAX_CALLS_PER_PARENT_AGENT = 3;
    private static final String PROVIDER_DEFAULT_MODEL = "gpt-4o";

    // 跟踪是否已添加追踪处理器
    private static boolean traceProcessorAdded = false;

    public ExecuteTurnService(ChatClient chatClient, ObjectMapper objectMapper,
                              ToolService toolService, MongoTemplate mongoTemplate, RagTool ragTool,
                              WebSearchTool webSearchTool) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.toolService = toolService;
        this.mongoTemplate = mongoTemplate;
        this.ragTool = ragTool;
        this.webSearchTool = webSearchTool;
    }

    /**
     * 模拟工具执行
     * 为指定的工具名称生成模拟响应
     */
    public String mockTool(String toolName, String args, String description, String mockInstructions) {
        try {
            log.info("为工具调用模拟响应: {}", toolName);

            List<Message> messages = Arrays.asList(
                    new SystemMessage(String.format("你正在模拟执行名为'%s'的工具。以下是工具的描述: %s。以下是模拟工具的指令: %s。生成一个真实的响应，就像工具实际执行了给定参数一样。",
                            toolName, description, mockInstructions)),
                    new UserMessage(String.format("为工具'%s'生成真实响应，参数为: %s。响应应该简洁并专注于工具实际返回的内容。", toolName, args))
            );

            log.info("为工具生成模拟响应: {}", toolName);
            String responseContent = null;
            responseContent = chatClient.prompt().messages(messages).call().content();
            return responseContent;
        } catch (Exception e) {
            log.error("mockTool中的错误: {}", e.getMessage());
            return String.format("错误: %s", e.getMessage());
        }
    }

    /**
     * 调用Webhook
     * 向指定的webhook URL发送工具调用请求
     */
    public String callWebhook(String toolName, String args, String webhookUrl, String signingSecret) {
        try {
            log.info("为工具调用webhook: {}", toolName);
            Map<String, Object> contentDict = Map.of(
                    "toolCall", Map.of(
                            "function", Map.of(
                                    "name", toolName,
                                    "arguments", args
                            )
                    )
            );
            Map<String, String> requestBody = Map.of("content", objectMapper.writeValueAsString(contentDict));

            // 准备请求头
            Map<String, String> headers = new HashMap<>();
            if (signingSecret != null && !signingSecret.isEmpty()) {
                String contentStr = requestBody.get("content");
                String bodyHash = org.apache.commons.codec.digest.DigestUtils.sha256Hex(contentStr);
                Map<String, String> payload = Map.of("bodyHash", bodyHash);
                String signatureJwt = generateJWT(payload, signingSecret);
                headers.put("X-Signature-Jwt", signatureJwt);
            }

            // 这里应该使用实际的HTTP客户端发送请求
            // 为了简化，我们返回模拟响应
            return "Webhook调用成功";
        } catch (Exception e) {
            log.error("callWebhook中的异常: {}", e.getMessage());
            return String.format("错误: 调用webhook失败 - %s", e.getMessage());
        }
    }

    /**
     * 调用MCP工具
     * 通过MCP协议调用远程工具
     */
    public String callMcp(String toolName, String args, String mcpServerUrl) {
        try {
            log.info("为工具调用MCP: {}，参数: {}，URL: {}", toolName, args, mcpServerUrl);
            // 这里应该实现实际的MCP客户端调用
            // 为了简化，我们返回模拟响应
            return String.format("MCP工具 %s 执行成功", toolName);
        } catch (Exception e) {
            log.error("callMcp中的错误: {}", e.getMessage());
            return String.format("错误: %s", e.getMessage());
        }
    }

    /**
     * 通用工具调用处理器
     * 根据工具配置决定调用方式（模拟、MCP或Webhook）
     */
    @Tool
    public String catchAll(Object ctx, String args, String toolName, Map<String, Object> toolConfig, Map<String, Object> completeRequest) {
        try {
            log.info("通用处理器为工具调用: {}", toolName);
            // 打印完整的工具调用信息
            log.info("工具调用详情:\n{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
                    "tool_name", toolName,
                    "arguments", args != null ? objectMapper.readValue(args, Map.class) : Map.of(),
                    "config", Map.of(
                            "description", toolConfig.getOrDefault("description", ""),
                            "isMcp", toolConfig.getOrDefault("isMcp", false),
                            "mcpServerName", toolConfig.getOrDefault("mcpServerName", ""),
                            "parameters", toolConfig.getOrDefault("parameters", Map.of())
                    )
            )));

            String responseContent = null;
            if (Boolean.TRUE.equals(toolConfig.get("mockTool")) ||
                    Boolean.TRUE.equals(((Map<String, Object>) completeRequest.getOrDefault("testProfile", Map.of())).get("mockTools"))) {
                // 调用mockTool处理响应
                String mockPrompt = (String) ((Map<String, Object>) completeRequest.getOrDefault("testProfile", Map.of())).get("mockPrompt");
                if (mockPrompt != null && !mockPrompt.isEmpty()) {
                    responseContent = mockTool(toolName, args,
                            (String) toolConfig.getOrDefault("description", ""), mockPrompt);
                } else {
                    responseContent = mockTool(toolName, args,
                            (String) toolConfig.getOrDefault("description", ""),
                            (String) toolConfig.getOrDefault("mockInstructions", ""));
                }
                log.info(responseContent);
            } else if (Boolean.TRUE.equals(toolConfig.get("isMcp"))) {
                String mcpServerUrl = (String) toolConfig.get("mcpServerURL");
                if (mcpServerUrl == null || mcpServerUrl.isEmpty()) {
                    // 向后兼容旧项目
                    String mcpServerName = (String) toolConfig.get("mcpServerName");
                    List<Map<String, Object>> mcpServers = (List<Map<String, Object>>) completeRequest.getOrDefault("mcpServers", List.of());
                    mcpServerUrl = mcpServers.stream()
                            .filter(server -> mcpServerName.equals(server.get("name")))
                            .map(server -> (String) server.get("url"))
                            .findFirst()
                            .orElse("");
                }

                responseContent = callMcp(toolName, args, mcpServerUrl);
            } else {
                // 从MongoDB获取项目信息
                String projectId = (String) completeRequest.get("projectId");
                // 这里应该查询MongoDB获取签名密钥
                String signingSecret = "mock_secret"; // 模拟值
                String webhookUrl = (String) completeRequest.get("toolWebhookUrl");
                responseContent = callWebhook(toolName, args, webhookUrl, signingSecret);
            }
            return responseContent;
        } catch (Exception e) {
            log.error("catchAll中的错误: {}", e.getMessage());
            return String.format("错误: %s", e.getMessage());
        }
    }

    /**
     * 基于提供的配置创建RAG工具
     */
    private ToolCallback getRagTool(AgentConfig config, Map<String, Object> completeRequest) {
        String projectId = (String) completeRequest.get("projectId");
        if (config.getAdditionalProperties() != null &&
                config.getAdditionalProperties().get("ragDataSources") != null) {
            log.info("创建rag_search工具，参数:\n-数据源: {}\n-返回类型: {}\n-K: {}",
                    config.getAdditionalProperties().get("ragDataSources"),
                    config.getAdditionalProperties().getOrDefault("ragReturnType", "chunks"),
                    config.getAdditionalProperties().getOrDefault("ragK", 3));

            Map<String, Object> params = Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "query", Map.of(
                                    "type", "string",
                                    "description", "要搜索的查询"
                            )
                    ),
                    "additionalProperties", false,
                    "required", List.of("query")
            );

            ToolCallback tool = MethodToolCallback.builder()
                    .toolDefinition(ToolDefinition.builder()
                            .name("rag_search")
                            .description("使用RAG数据源进行搜索")
                            .build())
                    .toolMethod(ReflectionUtils.findMethod(RagTool.class,
                            "callRagTool"))
                    .toolObject(new RagTool())
                    .build();
            return tool;
        } else {
            return null;
        }
    }

    /**
     * 基于配置和连接创建并初始化Agent对象
     */
    @SneakyThrows
    public List<AgentConfig> getAgents(List<AgentConfig> agentConfigs, List<ToolConfig> toolConfigs, Map<String, Object> completeRequest) {
        if (!(agentConfigs instanceof List)) {
            throw new IllegalArgumentException("get_agents中的代理配置不是列表");
        }
        if (!(toolConfigs instanceof List)) {
            throw new IllegalArgumentException("get_agents中的工具配置不是列表");
        }

        List<AgentConfig> newAgents = new ArrayList<>();
        Map<String, List<String>> newAgentToChildren = new HashMap<>();
        Map<String, Integer> newAgentNameToIndex = new HashMap<>();

        // 从配置创建Agent对象
        for (AgentConfig agentConfig : agentConfigs) {
            log.info("=".repeat(100));
            log.info("处理代理配置: {}", agentConfig.getName());

            // 如果有RAG数据源，将RAG工具添加到代理的工具列表中
            if (Boolean.TRUE.equals(agentConfig.getAdditionalProperties().get("hasRagSources"))) {
                String ragToolName = getToolConfigByType(toolConfigs, "rag").getName();
                agentConfig.getTools().add(ragToolName);
                agentConfig = addRagInstructionsToAgent(agentConfig, ragToolName);
            }

            // 为此代理准备工具列表
            List<String> externalTools = new ArrayList<>();

            log.info("代理 {} 有 {} 个配置的工具", agentConfig.getName(), agentConfig.getTools().size());

            List<ToolCallback> newTools = new ArrayList<>();

            for (String toolName : agentConfig.getTools()) {
                ToolConfig toolConfig = getToolConfigByName(toolConfigs, toolName);

                if (toolConfig != null) {
                    // 在工具参数中保留所有JSON Schema属性
                    Map<String, Object> toolParams = toolConfig.getParameters();
                    if (toolParams instanceof Map) {
                        // 确保我们保留模式中的所有属性
                        List<String> jsonSchemaProperties = Arrays.asList(
                                "enum", "default", "minimum", "maximum", "items", "format",
                                "pattern", "minLength", "maxLength", "minItems", "maxItems",
                                "uniqueItems", "multipleOf", "examples"
                        );
                        Map<String, Object> properties = (Map<String, Object>) toolParams.get("properties");
                        if (properties != null) {
                            for (String propName : properties.keySet()) {
                                Map<String, Object> propSchema = (Map<String, Object>) properties.get(propName);
                                if (propSchema != null) {
                                    // 复制所有现有的JSON Schema属性
                                    for (String schemaProp : jsonSchemaProperties) {
                                        if (propSchema.containsKey(schemaProp)) {
                                            propSchema.put(schemaProp, propSchema.get(schemaProp));
                                        }
                                    }
                                }
                            }
                        }
                    }

                    externalTools.add(toolName);

                    ToolCallback tool = null;
                    if ("web_search".equals(toolName)) {
                        Method method = ReflectionUtils.findMethod(WebSearchTool.class, "web_search");
                        ToolCallback toolCallback = MethodToolCallback.builder()
                                .toolDefinition(ToolDefinition.builder()
                                        .name("web_search")
                                        .description("搜索网络信息")
                                        .build())
                                .toolMethod(method)
                                .toolObject(webSearchTool)
                                .build();
                        newTools.add(tool);
                    } else if ("rag_search".equals(toolName)) {
                        tool = getRagTool(agentConfig, completeRequest);
                        newTools.add(tool);
                    } else {
                        tool = MethodToolCallback.builder()
                                .toolDefinition(ToolDefinition.builder()
                                        .name(toolName)
                                        .description(toolConfig.getDescription())
                                        .inputSchema(toolConfig.getParameters().toString())
                                        .build())
                                .toolMethod(ReflectionUtils.findMethod(ExecuteTurnService.class, "catchAll"))
                                .toolObject(this)
                                .build();
                        newTools.add(tool);
                    }
                    if (tool != null) {
                        newTools.add(tool);
                        log.info("为代理 {} 添加工具 {}", agentConfig.getName(), toolName);
                    }
                } else {
                    log.warn("警告: 在tool_configs中未找到工具 {}", toolName);
                }
            }

            // 创建代理对象
            log.info("为 {} 创建Agent对象", agentConfig.getName());

            // Create the agent object
            log.info("Creating Agent object for {}", agentConfig.getName());

            // Add the name and description to the agent instructions
            String agentInstructions = String.format("## Your Name\n%s\n\n## Description\n%s\n\n## Instructions\n%s",
                    agentConfig.getName(),
                    agentConfig.getDescription() != null ? agentConfig.getDescription() : "",
                    agentConfig.getInstructions() != null ? agentConfig.getInstructions() : "");

            try {
                // Identify the model
                String modelName = agentConfig.getModel() != null ? agentConfig.getModel() : PROVIDER_DEFAULT_MODEL;
                log.info("Using model: {}", modelName);

                // Set default values if not provided
                if (agentConfig.getMaxCallsPerTurn() == 0) {
                    agentConfig.setMaxCallsPerTurn(10);
                }
                if (agentConfig.getMaxTokensPerTurn() == 0) {
                    agentConfig.setMaxTokensPerTurn(4000);
                }
                if (agentConfig.getMaxTokensPerResponse() == 0) {
                    agentConfig.setMaxTokensPerResponse(2000);
                }
                if (agentConfig.getTemperature() == 0.0) {
                    agentConfig.setTemperature(0.7);
                }

                // Set the max calls per parent agent
                Integer maxCallsPerParentAgent = agentConfig.getAdditionalProperties() != null ?
                        (Integer) agentConfig.getAdditionalProperties().get("maxCallsPerParentAgent") : null;
                if (maxCallsPerParentAgent == null) {
                    maxCallsPerParentAgent = DEFAULT_MAX_CALLS_PER_PARENT_AGENT;
                    log.warn("WARNING: Max calls per parent agent not received for agent {}. Using rowboat_agents default of {}",
                            agentConfig.getName(), DEFAULT_MAX_CALLS_PER_PARENT_AGENT);
                } else {
                    log.info("Max calls per parent agent for agent {}: {}", agentConfig.getName(), maxCallsPerParentAgent);
                }

                // Set output visibility
                OutputVisibility outputVisibility = agentConfig.getOutputVisibility();
                if (outputVisibility == null) {
                    outputVisibility = OutputVisibility.USER_FACING;
                    log.warn("WARNING: Output visibility not received for agent {}. Using rowboat_agents default of {}",
                            agentConfig.getName(), outputVisibility.getValue());
                } else {
                    log.info("Output visibility for agent {}: {}", agentConfig.getName(), outputVisibility.getValue());
                }

                // Handle the connected agents
                List<String> connectedAgents = agentConfig.getConnectedAgents();
                if (connectedAgents == null) {
                    connectedAgents = new ArrayList<>();
                }
                newAgentToChildren.put(agentConfig.getName(), connectedAgents);
                newAgentNameToIndex.put(agentConfig.getName(), newAgents.size());

                // Update the agent config with processed values
                agentConfig.setInstructions(agentInstructions);
                agentConfig.setOutputVisibility(outputVisibility);

                // Store max calls per parent agent in additional properties
                if (agentConfig.getAdditionalProperties() == null) {
                    agentConfig.setAdditionalProperties(new HashMap<>());
                }
                agentConfig.getAdditionalProperties().put("maxCallsPerParentAgent", maxCallsPerParentAgent);

                newAgents.add(agentConfig);
                log.info("Successfully created agent: {}", agentConfig.getName());

            } catch (Exception e) {
                log.error("错误: 创建代理 {} 失败: {}", agentConfig.getName(), e.getMessage());
                throw new RuntimeException(e);
            }
        }

        log.info("返回创建的代理");
        log.info("=".repeat(100));
        return newAgents;
    }

    /**
     * 以流式模式初始化和运行Swarm客户端的包装函数
     */
    public Flux<StreamEvent> runStreamed(AgentConfig agent, List<Message> messages,
                                         List<String> externalTools, Map<String, Integer> tokensUsed,
                                         Boolean enableTracing) {
        log.info("为代理初始化流式客户端: {}", agent.getName());

        // 初始化默认参数
        if (externalTools == null) {
            externalTools = new ArrayList<>();
        }
        if (tokensUsed == null) {
            tokensUsed = new HashMap<>();
        }

        // 格式化消息以确保它们与OpenAI API兼容
        List<Message> formattedMessages = new ArrayList<>();

        log.info("开始流式运行");

        try {
            // 仅在启用追踪时添加我们的自定义追踪处理器
            if (enableTracing && !traceProcessorAdded) {
                AgentTurnTraceProcessor traceProcessor = new AgentTurnTraceProcessor();
                addTraceProcessor(traceProcessor);
                traceProcessorAdded = true;
            }

            // 首先在没有追踪上下文的情况下获取流结果
            StreamResult streamResult = Runner.runStreamed(agent, formattedMessages);

            // 如果启用追踪，包装stream_events以处理追踪
            if (enableTracing) {
                Flux<Event> originalStreamEvents = streamResult.getStreamEvents();

                Flux<Event> wrappedStreamEvents = Flux.create(sink -> {
                    // 在异步函数内部创建追踪上下文
                    try (TraceContext traceCtx = trace("代理回合: " + agent.getName())) {
                        try {
                            originalStreamEvents.subscribe(
                                    event -> sink.next(event),
                                    error -> {
                                        log.error("流事件中的错误: {}", error.getMessage());
                                        sink.error(error);
                                    },
                                    sink::complete
                            );
                        } catch (Exception e) {
                            log.error("流事件中的错误: {}", e.getMessage());
                            sink.error(e);
                        }
                    }
                });

                streamResult.setStreamEvents(wrappedStreamEvents);
            }

            return streamResult.getStreamEvents().map(this::convertToStreamEvent);
        } catch (Exception e) {
            log.error("流式运行期间出错: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // 辅助方法
    private ToolConfig getToolConfigByName(List<ToolConfig> toolConfigs, String toolName) {
        return toolConfigs.stream()
                .filter(tc -> toolName.equals(tc.getName()))
                .findFirst()
                .orElse(null);
    }

    private ToolConfig getToolConfigByType(List<ToolConfig> toolConfigs, String toolType) {
        return toolConfigs.stream()
                .filter(tc -> toolType.equals(tc.getType()))
                .findFirst()
                .orElse(null);
    }

    private AgentConfig addRagInstructionsToAgent(AgentConfig agentConfig, String ragToolName) {
        // 实现RAG指令添加逻辑
        return agentConfig;
    }


    private String generateJWT(Map<String, String> payload, String secret) {
        // 实现JWT生成逻辑
        return "生成的JWT令牌";
    }

    private StreamEvent convertToStreamEvent(Event event) {
        // 实现事件转换逻辑
        return new StreamEvent(event.getType(), event.getData());
    }

    // 假设的类和方法（需要根据实际项目结构调整）
    private static class OpenAIChatCompletionsModel {
        public OpenAIChatCompletionsModel(String modelName, Object client) {
        }
    }

    private static class Runner {
        public static StreamResult runStreamed(AgentConfig agent, List<Message> messages) {
            return new StreamResult();
        }
    }

    private static class StreamResult {
        private Flux<Event> streamEvents;

        public Flux<Event> getStreamEvents() {
            return streamEvents;
        }

        public void setStreamEvents(Flux<Event> events) {
            this.streamEvents = events;
        }
    }

    private static class Event {
        private String type;
        private Object data;

        public String getType() {
            return type;
        }

        public Object getData() {
            return data;
        }
    }

    private static class StreamEvent {
        private String type;
        private Object data;

        public StreamEvent(String type, Object data) {
            this.type = type;
            this.data = data;
        }
    }

    private static class TraceContext implements AutoCloseable {
        public TraceContext(String name) {
        }

        @Override
        public void close() {
        }
    }

    private static TraceContext trace(String name) {
        return new TraceContext(name);
    }

    private static void addTraceProcessor(Object processor) {
    }

    private static class AgentTurnTraceProcessor {
    }

    private static Object client = null;
    private static Object asyncClientV1 = null;
}

