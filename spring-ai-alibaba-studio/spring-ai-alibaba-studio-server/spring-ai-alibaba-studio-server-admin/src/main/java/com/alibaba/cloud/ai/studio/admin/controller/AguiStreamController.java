/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.studio.admin.controller;

import com.alibaba.cloud.ai.graph.event.context.Context;
import com.alibaba.cloud.ai.graph.event.agent.RunAgentInput;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.event.message.BaseMessage;
import com.alibaba.cloud.ai.graph.event.message.MessageMapper;
import com.alibaba.cloud.ai.graph.event.state.State;
import com.alibaba.cloud.ai.graph.event.tool.Tool;
import com.alibaba.cloud.ai.graph.event.tool.ToolCall;
import com.alibaba.cloud.ai.studio.admin.agent.AgentOrchestrator;
import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import com.alibaba.cloud.ai.graph.event.event.BaseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import static java.lang.String.format;

@RestController
@RequestMapping("/")
@Tag(name = "CopilotKit ServiceAdapter Controller", description = "CopilotKit ServiceAdapter streaming controller")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class AguiStreamController {

    private static final Logger log = LoggerFactory.getLogger(AguiStreamController.class);

    @Resource
    private ChatModel chatModel;

    private final MessageMapper messageMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AguiStreamController() {
        this.messageMapper = new MessageMapper();
    }


    @PostMapping(path = "/copilotkit", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "CopilotKit ServiceAdapter Endpoint", description = "Main ServiceAdapter endpoint for CopilotKit integration")
    public Flux<ServerSentEvent<BaseEvent>> copilotKitServiceAdapter(@RequestBody String requestBody, HttpServletResponse response) throws Exception {
        log.info("ServiceAdapter request received: {}", requestBody);

        // Set CORS headers
        setCorsHeaders(response);

        // Parse JSON input and convert to RunAgentInput
        com.alibaba.fastjson.JSONObject jsonObject = JSON.parseObject(requestBody);
        RunAgentInput input = createAdaptedRunAgentInput(jsonObject);

        // Convert tools and messages
        List<ToolCallback> toolCallbacks = convertToolsToToolCallbacks(input.tools());
        List<org.springframework.ai.chat.messages.AbstractMessage> springMessages
                = convertMessagesToSpringMessages(input.messages());

        AgentOrchestrator agentOrchestrator = new AgentOrchestrator();

        return agentOrchestrator.run(callbackManager -> {
            // Create ReactAgent
            try {
                ReactAgent agent = ReactAgent.builder()
                        .name("agui_stream_agent")
                        .model(chatModel)
                        .inputKey("llm_input_messages")
                        .tools(toolCallbacks)
                        .callManager(callbackManager)
                        .build();

                // Build graph inputs
                Map<String, Object> graphInputs = new HashMap<>();
                graphInputs.put("llm_input_messages", springMessages);
                graphInputs.put("threadId", input.threadId());
                graphInputs.put("runId", input.runId());
                graphInputs.put("tools", input.tools());
                graphInputs.put("context", input.context());

                // Execute the agent
                agent.invoke(graphInputs);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        },input);

    }


    /**
     * Health check endpoint
     */
    @GetMapping(path = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("service", "CopilotKit ServiceAdapter Controller");
        health.put("mode", "service_adapter");
        health.put("endpoint", "/copilotkit");
        health.put("timestamp", new java.util.Date().toString());
        return ResponseEntity.ok(health);
    }

    // ========== Helper Methods ==========

    /**
     * Set CORS headers for all responses
     */
    private void setCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setHeader("Access-Control-Allow-Credentials", "true");
    }


    /**
     * Execute weather tool with parsed parameters
     */
    private String executeWeatherTool(String requestBody) {
        try {
            com.alibaba.fastjson.JSONObject request = JSON.parseObject(requestBody);
            String city = request.getString("city");
            String timestamp = request.getString("currentTimestamp");

            WeatherTool weatherTool = new WeatherTool();
            return weatherTool.getWeather(city != null ? city : "Unknown", timestamp != null ? timestamp : String.valueOf(System.currentTimeMillis()));

        } catch (Exception e) {
            log.error("Error executing weather tool", e);
            return "{\"error\": \"Failed to execute weather tool\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }

    // ========== Legacy Support Methods ==========

    /**
     * 转换 RunAgentInput 的 tools 到 ReactAgent 认可的 ToolCallback 格式
     */
    private List<ToolCallback> convertToolsToToolCallbacks(List<Tool> tools) {
        if (tools == null || tools.isEmpty()) {
            // 如果没有传入工具，只返回默认的weather tool

        }
        ToolCallback[] weatherToolCallback = ToolCallbacks.from(new WeatherTool());
        return List.of(weatherToolCallback);

//        List<ToolCallback> toolCallbacks = new ArrayList<>();
//        Set<String> registeredToolNames = new HashSet<>(); // 用于防止重复注册
//
//        for (Tool tool : tools) {
//            // 检查是否已经注册过相同名称的工具
//            if (registeredToolNames.contains(tool.name())) {
//                log.warn("工具 {} 已经注册，跳过重复注册", tool.name());
//                continue;
//            }
//
//            // 为每个 Tool 创建一个 FunctionToolCallback
//            FunctionToolCallback toolCallback = FunctionToolCallback.builder(tool.name(), (String input) -> {
//                        // 这里可以实现具体的工具执行逻辑
//                        log.info("工具 {} 被调用，参数: {}", tool.name(), input);
//                        return "Tool " + tool.name() + " executed with input: " + input;
//                    })
//                    .description(tool.description())
//                    .inputType(String.class)
//                    .build();
//
//            toolCallbacks.add(toolCallback);
//            registeredToolNames.add(tool.name());
//        }


//        log.info("注册的工具: {}", registeredToolNames);
//        return toolCallbacks;
    }

    /**
     * 创建适配的RunAgentInput，处理字段映射
     */
    private RunAgentInput createAdaptedRunAgentInput(com.alibaba.fastjson.JSONObject jsonObject) {
        String threadId = jsonObject.getString("threadId");
        String runId = jsonObject.getString("runId");

        // 转换 messages
        List<BaseMessage> messages = convertJsonMessages(jsonObject.getJSONArray("messages"));

        // 转换 actions 到 tools
        List<Tool> tools = convertJsonActions(jsonObject.getJSONArray("actions"));

        // 创建空的 State
        State state = new State();

        // 转换 agentStates 到 State
        if (jsonObject.getJSONArray("agentStates") != null) {
            // 这里可以根据需要处理 agentStates
            System.out.println("处理 agentStates: " + jsonObject.getJSONArray("agentStates"));
        }

        // 转换 forwardedParameters 到 forwardedProps
        Object forwardedProps = jsonObject.get("forwardedParameters");

        // 创建空的 context 列表
        List<Context> context = new ArrayList<>();

        return new RunAgentInput(threadId, runId, state, messages, tools, context, forwardedProps);
    }

    /**
     * 转换JSON messages到BaseMessage列表
     */
    private List<BaseMessage> convertJsonMessages(com.alibaba.fastjson.JSONArray messagesArray) {
        if (messagesArray == null || messagesArray.isEmpty()) {
            return new ArrayList<>();
        }

        List<BaseMessage> messages = new ArrayList<>();
        for (int i = 0; i < messagesArray.size(); i++) {
            com.alibaba.fastjson.JSONObject msgObj = messagesArray.getJSONObject(i);
            // 根据role创建对应的消息类型
            BaseMessage message = createMessageByRole(msgObj);
            if (message != null) {
                messages.add(message);
            }
        }

        return messages;
    }

    /**
     * 转换JSON actions到Tool列表
     */
    private List<Tool> convertJsonActions(com.alibaba.fastjson.JSONArray actionsArray) {
        if (actionsArray == null || actionsArray.isEmpty()) {
            return new ArrayList<>();
        }

        List<Tool> tools = new ArrayList<>();
        for (int i = 0; i < actionsArray.size(); i++) {
            com.alibaba.fastjson.JSONObject actionObj = actionsArray.getJSONObject(i);
            String name = actionObj.getString("name");
            String description = actionObj.getString("description");
            String jsonSchema = actionObj.getString("jsonSchema");

            // 解析JSON Schema并创建ToolParameters
            Tool.ToolParameters parameters = parseJsonSchema(jsonSchema);

            Tool tool = new Tool(name, description, parameters);
            tools.add(tool);
        }

        return tools;
    }

    /**
     * 解析JSON Schema并创建ToolParameters
     */
    private Tool.ToolParameters parseJsonSchema(String jsonSchema) {
        try {
            // 检查jsonSchema是否为null或空
            if (jsonSchema == null || jsonSchema.trim().isEmpty()) {
                log.warn("JSON Schema is null or empty, returning default parameters");
                return new Tool.ToolParameters("object", new HashMap<>(), new ArrayList<>());
            }

            com.alibaba.fastjson.JSONObject schema = JSON.parseObject(jsonSchema);
            if (schema == null) {
                log.warn("Failed to parse JSON Schema, returning default parameters");
                return new Tool.ToolParameters("object", new HashMap<>(), new ArrayList<>());
            }

            String type = schema.getString("type");
            if (type == null) {
                type = "object";
            }

            // 解析properties
            Map<String, Tool.ToolProperty> properties = new HashMap<>();
            com.alibaba.fastjson.JSONObject props = schema.getJSONObject("properties");
            if (props != null) {
                for (String key : props.keySet()) {
                    com.alibaba.fastjson.JSONObject prop = props.getJSONObject(key);
                    if (prop != null) {
                        String propType = prop.getString("type");
                        String propDesc = prop.getString("description");
                        if (propType == null) propType = "string";
                        if (propDesc == null) propDesc = "";

                        properties.put(key, new Tool.ToolProperty(propType, propDesc));
                    }
                }
            }

            // 解析required
            List<String> required = new ArrayList<>();
            com.alibaba.fastjson.JSONArray reqArray = schema.getJSONArray("required");
            if (reqArray != null) {
                for (int i = 0; i < reqArray.size(); i++) {
                    String reqField = reqArray.getString(i);
                    if (reqField != null) {
                        required.add(reqField);
                    }
                }
            }

            return new Tool.ToolParameters(type, properties, required);

        } catch (Exception e) {
            log.error("解析JSON Schema失败: {}", e.getMessage(), e);
            // 返回默认值
            return new Tool.ToolParameters("object", new HashMap<>(), new ArrayList<>());
        }
    }

    /**
     * 转换 RunAgentInput 的 messages 到 React AI 认可的 Message 格式
     */
    private List<org.springframework.ai.chat.messages.AbstractMessage> convertMessagesToSpringMessages(List<BaseMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        return messages.stream().map(messageMapper::toSpringMessage).toList();
    }

    /**
     * 根据JSONObject创建对应的消息类型
     */
    private BaseMessage createMessageByRole(com.alibaba.fastjson.JSONObject msgObj) {
        BaseMessage message = null;
        String role = msgObj.getString("role");
        String content = msgObj.getString("content");
        String id = msgObj.getString("id");

        switch (role.toLowerCase()) {
            case "user":
                message = new com.alibaba.cloud.ai.graph.event.message.UserMessage(id, content, "");
                break;
            case "assistant":
                List<com.alibaba.cloud.ai.graph.event.tool.ToolCall> toolCalls = new ArrayList<>();
                if (msgObj.containsKey("toolCalls") && msgObj.getJSONArray("toolCalls") != null) {
                    com.alibaba.fastjson.JSONArray toolCallsArray = msgObj.getJSONArray("toolCalls");
                    if (!toolCallsArray.isEmpty()) {
                        toolCalls = JSON.parseArray(toolCallsArray.toJSONString(), ToolCall.class);
                    }
                }
                message = new com.alibaba.cloud.ai.graph.event.message.AssistantMessage(id, content, "", toolCalls);
                break;
            case "system":
                message = new com.alibaba.cloud.ai.graph.event.message.SystemMessage(id, content, "");
                break;
            case "developer":
                message = new com.alibaba.cloud.ai.graph.event.message.DeveloperMessage(id, content, "");
                break;
            case "tool":
                message = new com.alibaba.cloud.ai.graph.event.message.ToolMessage(id, content, "",
                        msgObj.getString("toolCallId")
                        , msgObj.getString("error"));
                break;
            default:
                System.err.println("未知的消息角色: " + role + "，使用UserMessage作为默认值");
                message = new com.alibaba.cloud.ai.graph.event.message.UserMessage(id, content, "");
                break;
        }

        System.out.println("创建消息 - role: " + role + ", id: " + id + ", content: " + content);
        return message;
    }

    /**
     * 天气工具类，用于演示工具的实际调用
     */
    public static class WeatherTool {

        @org.springframework.ai.tool.annotation.Tool(name = "weather_tool", description = "获取指定城市的天气信息")
        public String getWeather(@ToolParam(description = "城市名称") String city,
                                 @ToolParam(description = "当前时间戳") String currentTimestamp) {
            System.out.println("==TOOL被调用==");
            return String.format("{\"city\": \"%s\", \"temperature\": -10, \"time\": \"%s\"}", city, currentTimestamp);
        }

        @org.springframework.ai.tool.annotation.Tool(description = "Send an email to someone")
        public String sendEmail(
                @ToolParam(description = "destination address") String to,
                @ToolParam(description = "subject of the email") String subject,
                @ToolParam(description = "body of the email") String body
        ) {
            // This is a placeholder for the actual implementation
            return format("mail sent to %s with subject %s", to, subject);
        }

    }
}