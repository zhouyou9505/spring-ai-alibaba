///*
// * Copyright 2024-2025 the original author or authors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.alibaba.cloud.ai.studio.admin.controller;
//
//import com.alibaba.cloud.ai.graph.event.context.Context;
//import com.alibaba.cloud.ai.graph.event.manager.CallbackManager;
//import com.alibaba.cloud.ai.graph.event.manager.CallbackManagerImpl;
//import com.alibaba.cloud.ai.graph.event.agent.RunAgentInput;
//import com.alibaba.cloud.ai.graph.CompiledGraph;
//import com.alibaba.cloud.ai.graph.agent.ReactAgent;
//import com.alibaba.cloud.ai.graph.event.manager.EventHandler;
//import com.alibaba.cloud.ai.graph.event.message.BaseMessage;
//import com.alibaba.cloud.ai.graph.event.message.MessageMapper;
//import com.alibaba.cloud.ai.graph.event.state.State;
//import com.alibaba.cloud.ai.graph.event.tool.Tool;
//import com.alibaba.cloud.ai.graph.event.tool.ToolCall;
//import com.alibaba.fastjson.JSON;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.annotation.Resource;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.ai.support.ToolCallbacks;
//import org.springframework.ai.tool.annotation.ToolParam;
//import org.springframework.http.MediaType;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//import org.springframework.ai.tool.ToolCallback;
//import org.springframework.ai.tool.function.FunctionToolCallback;
//import org.springframework.ai.chat.model.ChatModel;
//
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.List;
//import java.util.ArrayList;
//import java.util.concurrent.CompletableFuture;
//
//@RestController
//@RequestMapping("")
//@Tag(name = "AGUI Stream Controller", description = "AGUI streaming chat controller")
//public class AguiStreamController {
//
//    private static final Logger log = LoggerFactory.getLogger(AguiStreamController.class);
//    @Resource
//    private ChatModel chatModel; // 注入 ChatModel
//
//    private final MessageMapper messageMapper;
//
//    public AguiStreamController() {
//        this.messageMapper = new MessageMapper();
//    }
//
//    // 使用 ThreadLocal 存储当前会话的 SseEmitter
//    private final ThreadLocal<SseEmitter> currentEmitter = new ThreadLocal<>();
//
//
//
//    @RequestMapping(path = "/copilotkit", consumes = MediaType.APPLICATION_JSON_VALUE,
//            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    @Operation(summary = "Start streaming chat session",
//            description = "Initiates a real-time streaming chat session with AI agent")
//    public SseEmitter copilotKit(@RequestBody String inputStr) throws Exception {
//
//        // 打印入参用于调试
//        System.out.println("收到请求参数: " + inputStr);
//
//        // 解析JSON并转换字段映射
//        com.alibaba.fastjson.JSONObject jsonObject = JSON.parseObject(inputStr);
//
//        // 创建适配的RunAgentInput
//        RunAgentInput input = createAdaptedRunAgentInput(jsonObject);
//
//        // 创建 SseEmitter
//        SseEmitter emitter = new SseEmitter(0L); // 无超时
//        String threadId = input.threadId();
//
//        // 设置当前线程的 SseEmitter
//        currentEmitter.set(emitter);
//
//        // 设置 SseEmitter 的回调，清理 ThreadLocal
//        emitter.onCompletion(() -> {
//            currentEmitter.remove();
//        });
//
//        emitter.onError(throwable -> {
//            currentEmitter.remove();
//        });
//
//        emitter.onTimeout(() -> {
//            currentEmitter.remove();
//        });
//
//        // 转换 tools 从 RunAgentInput 到 ReactAgent 认可的 ToolCallback 格式
//        List<ToolCallback> toolCallbacks = convertToolsToToolCallbacks(input.tools());
//
//        // 转换 messages 从 RunAgentInput 到 ReactAgent 认可的 Spring AI Message 格式
//        List<org.springframework.ai.chat.messages.AbstractMessage> springMessages
//                = convertMessagesToSpringMessages(input.messages());
//        // 创建回调管理器，传入 EventHandler 实例
//        CallbackManager callbackManager = new CallbackManagerImpl(new EventHandler(event -> {
//            if (emitter == null) {
//                return;
//            }
//            try {
//                log.info(JSON.toJSONString(event));
//                emitter.send(SseEmitter.event()
//                        .name(event.getClass().getSimpleName())
//                        .data(event));
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }));
//
//
//        // 创建 ReactAgent - 学习 ReactAgentHookTest.java 的完整初始化方式
//        ReactAgent agent = ReactAgent.builder()
//                .name("agui_stream_agent")
//                .model(chatModel)
//                .inputKey("llm_input_messages") // 设置输入键
//                .tools(toolCallbacks)
//                .callManager(callbackManager)
//                .build();
//
//        // 获取 CompiledGraph 并设置回调管理器
//        CompiledGraph graph = agent.getAndCompileGraph();
//        if (graph != null) {
//            graph.setCallbackManager(callbackManager);
//        }
//
//        // 异步启动 ReactAgent 执行
//        CompletableFuture.runAsync(() -> {
//            try {
//                // 构建输入参数 - 按照 ReactAgentHookTest.java 的方式
//                Map<String, Object> graphInputs = new HashMap<>();
//                graphInputs.put("llm_input_messages", springMessages); // 使用转换后的 Spring AI messages
//                graphInputs.put("threadId", input.threadId());
//                graphInputs.put("runId", input.runId());
//                graphInputs.put("tools", input.tools());
//                graphInputs.put("context", input.context());
//
//                agent.invoke(graphInputs);
//
//            } catch (Exception e) {
//                try {
//                    emitter.completeWithError(e);
//                } catch (Exception ex) {
//                    // 静默处理
//                }
//            } finally {
//                // 确保清理 ThreadLocal
//                currentEmitter.remove();
//            }
//        });
//
//        return emitter;
//    }
//
//    /**
//     * CopilotKit info endpoint - 返回agents和actions信息
//     * 这个端点是CopilotKit初始化时必需的
//     */
//    @RequestMapping(path = "/copilotkit/info", produces = MediaType.APPLICATION_JSON_VALUE)
//    @Operation(summary = "Get CopilotKit agents and actions information",
//            description = "Returns the agents and actions configuration that CopilotKit needs")
//    public Map<String, Object> getCopilotKitInfo() {
//        try {
//            Map<String, Object> info = new HashMap<>();
//
//            // 添加agents信息
//            Map<String, Object> agent = new HashMap<>();
//            agent.put("name", "ai_researcher");
//            agent.put("description", "AGUI Stream Agent for handling chat sessions");
//            agent.put("type", "react");
//
//            info.put("agents", new Object[]{agent});
//
//            // 添加actions信息 - 这些是工具/函数定义
//            List<Map<String, Object>> actions = new ArrayList<>();
//
//            // 添加天气工具作为action
//            Map<String, Object> weatherAction = new HashMap<>();
//            weatherAction.put("name", "weather_tool");
//            weatherAction.put("description", "获取指定城市的天气信息");
//            weatherAction.put("type", "function");
//
//            // 定义参数schema
//            Map<String, Object> parameters = new HashMap<>();
//            parameters.put("type", "object");
//
//            Map<String, Object> properties = new HashMap<>();
//
//            Map<String, Object> cityParam = new HashMap<>();
//            cityParam.put("type", "string");
//            cityParam.put("description", "城市名称");
//            properties.put("city", cityParam);
//
//            Map<String, Object> timestampParam = new HashMap<>();
//            timestampParam.put("type", "string");
//            timestampParam.put("description", "当前时间戳");
//            properties.put("currentTimestamp", timestampParam);
//
//            parameters.put("properties", properties);
//            parameters.put("required", new String[]{"city", "currentTimestamp"});
//
//            weatherAction.put("parameters", parameters);
//            actions.add(weatherAction);
//
//            info.put("actions", actions);
//
//            // 添加其他必要信息
//            info.put("version", "1.0.0");
//            info.put("status", "active");
//
//            return info;
//
//        } catch (Exception e) {
//            log.error("Error getting CopilotKit info", e);
//
//            // 返回错误响应，但确保包含必要的字段
//            Map<String, Object> errorResponse = new HashMap<>();
//            errorResponse.put("error", "Failed to get CopilotKit info");
//            errorResponse.put("message", e.getMessage());
//            errorResponse.put("agents", new Object[0]);
//            errorResponse.put("actions", new Object[0]);
//            errorResponse.put("status", "error");
//
//            return errorResponse;
//        }
//    }
//
//    /**
//     * 健康检查端点
//     */
//    @GetMapping(path = "/copilotkit/health", produces = MediaType.APPLICATION_JSON_VALUE)
//    public Map<String, String> health() {
//        Map<String, String> health = new HashMap<>();
//        health.put("status", "healthy");
//        health.put("service", "AGUI Stream Controller");
//        health.put("timestamp", new java.util.Date().toString());
//        return health;
//    }
//
//    /**
//     * CopilotKit agent state loading endpoint
//     * 处理CopilotKit的loadAgentState请求，返回必要的状态信息
//     */
//    @PostMapping(path = "/copilotkit/state", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
//    @Operation(summary = "Load agent state for CopilotKit",
//            description = "Handles CopilotKit's loadAgentState request and returns agent state information")
//    public Map<String, Object> loadAgentState(@RequestBody String requestBody) {
//        try {
//            log.info("Received loadAgentState request: {}", requestBody);
//
//            // 解析请求体
//            com.alibaba.fastjson.JSONObject request = JSON.parseObject(requestBody);
//
//            // 创建响应结构
//            Map<String, Object> response = new HashMap<>();
//
//            // 确保threadId不为null - 这是GraphQL错误的关键
//            String threadId = request.getString("threadId");
//            if (threadId == null || threadId.trim().isEmpty()) {
//                threadId = "default-thread-" + System.currentTimeMillis();
//                log.warn("threadId was null or empty, generated default: {}", threadId);
//            }
//
//            response.put("threadId", threadId);
//            response.put("runId", request.getString("runId"));
//
//            // 添加agent状态信息
//            Map<String, Object> agentState = new HashMap<>();
//            agentState.put("name", "ai_researcher");
//            agentState.put("status", "active");
//            agentState.put("lastActivity", new java.util.Date().toString());
//
//            response.put("agentState", agentState);
//
//            // 添加消息历史（如果有的话）
//            response.put("messages", new ArrayList<>());
//
//            // 添加工具状态
//            response.put("tools", new ArrayList<>());
//
//            // 添加成功状态
//            response.put("status", "success");
//            response.put("timestamp", new java.util.Date().toString());
//
//            log.info("Returning agent state response: {}", response);
//            return response;
//
//        } catch (Exception e) {
//            log.error("Error loading agent state", e);
//
//            // 返回错误响应，但确保threadId不为null
//            Map<String, Object> errorResponse = new HashMap<>();
//            errorResponse.put("error", "Failed to load agent state");
//            errorResponse.put("message", e.getMessage());
//            errorResponse.put("threadId", "error-thread-" + System.currentTimeMillis());
//            errorResponse.put("status", "error");
//            errorResponse.put("timestamp", new java.util.Date().toString());
//
//            return errorResponse;
//        }
//    }
//
//    /**
//     * 转换 RunAgentInput 的 tools 到 ReactAgent 认可的 ToolCallback 格式
//     */
//    private List<ToolCallback> convertToolsToToolCallbacks(List<Tool> tools) {
//        if (tools == null || tools.isEmpty()) {
//            return List.of();
//        }
//
//        List<ToolCallback> toolCallbacks = new ArrayList<>();
//
//        for (Tool tool : tools) {
//            // 为每个 Tool 创建一个 FunctionToolCallback
//            FunctionToolCallback toolCallback = FunctionToolCallback.builder(tool.name(), (String input) -> {
//                        // 这里可以实现具体的工具执行逻辑
//                        // 暂时返回一个占位符响应
//                        return "Tool " + tool.name() + " executed with input: " + input;
//                    })
//                    .description(tool.description())
//                    .inputType(String.class)
//                    .build();
//
//            toolCallbacks.add(toolCallback);
//        }
//        ToolCallback weatherToolCallback = ToolCallbacks.from(new WeatherTool())[0];
//        toolCallbacks.add(weatherToolCallback);
//        return toolCallbacks;
//    }
//
//    /**
//     * 创建适配的RunAgentInput，处理字段映射
//     */
//    private RunAgentInput createAdaptedRunAgentInput(com.alibaba.fastjson.JSONObject jsonObject) {
//        String threadId = jsonObject.getString("threadId");
//        String runId = jsonObject.getString("runId");
//
//        // 转换 messages
//        List<BaseMessage> messages = convertJsonMessages(jsonObject.getJSONArray("messages"));
//
//        // 转换 actions 到 tools
//        List<Tool> tools = convertJsonActions(jsonObject.getJSONArray("actions"));
//
//        // 创建空的 State
//        State state = new State();
//
//        // 转换 agentStates 到 State
//        if (jsonObject.getJSONArray("agentStates") != null) {
//            // 这里可以根据需要处理 agentStates
//            System.out.println("处理 agentStates: " + jsonObject.getJSONArray("agentStates"));
//        }
//
//        // 转换 forwardedParameters 到 forwardedProps
//        Object forwardedProps = jsonObject.get("forwardedParameters");
//
//        // 创建空的 context 列表
//        List<Context> context = new ArrayList<>();
//
//        return new RunAgentInput(threadId, runId, state, messages, tools, context, forwardedProps);
//    }
//
//    /**
//     * 转换JSON messages到BaseMessage列表
//     */
//    private List<BaseMessage> convertJsonMessages(com.alibaba.fastjson.JSONArray messagesArray) {
//        if (messagesArray == null || messagesArray.isEmpty()) {
//            return new ArrayList<>();
//        }
//
//        List<BaseMessage> messages = new ArrayList<>();
//        for (int i = 0; i < messagesArray.size(); i++) {
//            com.alibaba.fastjson.JSONObject msgObj = messagesArray.getJSONObject(i);
//            // 根据role创建对应的消息类型
//            BaseMessage message = createMessageByRole(msgObj);
//            if (message != null) {
//                messages.add(message);
//            }
//        }
//
//        return messages;
//    }
//
//    /**
//     * 转换JSON actions到Tool列表
//     */
//    private List<Tool> convertJsonActions(com.alibaba.fastjson.JSONArray actionsArray) {
//        if (actionsArray == null || actionsArray.isEmpty()) {
//            return new ArrayList<>();
//        }
//
//        List<Tool> tools = new ArrayList<>();
//        for (int i = 0; i < actionsArray.size(); i++) {
//            com.alibaba.fastjson.JSONObject actionObj = actionsArray.getJSONObject(i);
//            String name = actionObj.getString("name");
//            String description = actionObj.getString("description");
//            String jsonSchema = actionObj.getString("jsonSchema");
//
//            // 解析JSON Schema并创建ToolParameters
//            Tool.ToolParameters parameters = parseJsonSchema(jsonSchema);
//
//            Tool tool = new Tool(name, description, parameters);
//            tools.add(tool);
//        }
//
//        return tools;
//    }
//
//    /**
//     * 解析JSON Schema并创建ToolParameters
//     */
//    private Tool.ToolParameters parseJsonSchema(String jsonSchema) {
//        try {
//            com.alibaba.fastjson.JSONObject schema = JSON.parseObject(jsonSchema);
//            String type = schema.getString("type");
//
//            // 解析properties
//            Map<String, Tool.ToolProperty> properties = new HashMap<>();
//            com.alibaba.fastjson.JSONObject props = schema.getJSONObject("properties");
//            if (props != null) {
//                for (String key : props.keySet()) {
//                    com.alibaba.fastjson.JSONObject prop = props.getJSONObject(key);
//                    String propType = prop.getString("type");
//                    String propDesc = prop.getString("description");
//                    if (propDesc == null) propDesc = "";
//
//                    properties.put(key, new Tool.ToolProperty(propType, propDesc));
//                }
//            }
//
//            // 解析required
//            List<String> required = new ArrayList<>();
//            com.alibaba.fastjson.JSONArray reqArray = schema.getJSONArray("required");
//            if (reqArray != null) {
//                for (int i = 0; i < reqArray.size(); i++) {
//                    required.add(reqArray.getString(i));
//                }
//            }
//
//            return new Tool.ToolParameters(type, properties, required);
//        } catch (Exception e) {
//            System.err.println("解析JSON Schema失败: " + e.getMessage());
//            // 返回默认值
//            return new Tool.ToolParameters("object", new HashMap<>(), new ArrayList<>());
//        }
//    }
//
//    /**
//     * 转换 RunAgentInput 的 messages 到 React AI 认可的 Message 格式
//     */
//    private List<org.springframework.ai.chat.messages.AbstractMessage> convertMessagesToSpringMessages(List<BaseMessage> messages) {
//        if (messages == null || messages.isEmpty()) {
//            return List.of();
//        }
//        return messages.stream().map(messageMapper::toSpringMessage).toList();
//    }
//
//    /**
//     * 根据JSONObject创建对应的消息类型
//     */
//    private BaseMessage createMessageByRole(com.alibaba.fastjson.JSONObject msgObj) {
//        BaseMessage message = null;
//        String role = msgObj.getString("role");
//        String content = msgObj.getString("content");
//        String id = msgObj.getString("id");
//
//        switch (role.toLowerCase()) {
//            case "user":
//                message = new com.alibaba.cloud.ai.graph.event.message.UserMessage(id, content, "");
//                break;
//            case "assistant":
//                List<com.alibaba.cloud.ai.graph.event.tool.ToolCall> toolCalls = new ArrayList<>();
//                if (msgObj.containsKey("toolCalls") && msgObj.getJSONArray("toolCalls") != null) {
//                    com.alibaba.fastjson.JSONArray toolCallsArray = msgObj.getJSONArray("toolCalls");
//                    if (!toolCallsArray.isEmpty()) {
//                        toolCalls = JSON.parseArray(toolCallsArray.toJSONString(), ToolCall.class);
//                    }
//                }
//                message = new com.alibaba.cloud.ai.graph.event.message.AssistantMessage(id, content, "", toolCalls);
//                break;
//            case "system":
//                message = new com.alibaba.cloud.ai.graph.event.message.SystemMessage(id, content, "");
//                break;
//            case "developer":
//                message = new com.alibaba.cloud.ai.graph.event.message.DeveloperMessage(id, content, "");
//                break;
//            case "tool":
//                message = new com.alibaba.cloud.ai.graph.event.message.ToolMessage(id, content, "",
//                        msgObj.getString("toolCallId")
//                        , msgObj.getString("error"));
//                break;
//            default:
//                System.err.println("未知的消息角色: " + role + "，使用UserMessage作为默认值");
//                message = new com.alibaba.cloud.ai.graph.event.message.UserMessage(id, content, "");
//                break;
//        }
//
//        System.out.println("创建消息 - role: " + role + ", id: " + id + ", content: " + content);
//        return message;
//    }
//
//
//
//    /**
//     * 天气工具类，用于演示工具的实际调用
//     */
//    public static class WeatherTool {
//
//        @org.springframework.ai.tool.annotation.Tool(name = "weather_tool", description = "获取指定城市的天气信息")
//        public String getWeather(@ToolParam(description = "城市名称") String city,
//                                 @ToolParam(description = "当前时间戳") String currentTimestamp) {
//            System.out.println("==TOOL被调用==");
//            return String.format("{\"city\": \"%s\", \"temperature\": -50, \"time\": \"%s\"}", city, currentTimestamp);
//        }
//
//    }
//}
