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
import com.alibaba.cloud.ai.graph.event.manager.CallbackManager;
import com.alibaba.cloud.ai.graph.event.manager.CallbackManagerImpl;
import com.alibaba.cloud.ai.graph.event.agent.RunAgentInput;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.event.manager.EventHandler;
import com.alibaba.cloud.ai.graph.event.message.BaseMessage;
import com.alibaba.cloud.ai.graph.event.message.MessageMapper;
import com.alibaba.cloud.ai.graph.event.state.State;
import com.alibaba.cloud.ai.graph.event.tool.Tool;
import com.alibaba.cloud.ai.graph.event.tool.ToolCall;
import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.chat.model.ChatModel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/copilotkit")
@Tag(name = "CopilotKit MCP Controller", description = "CopilotKit MCP-compliant streaming controller")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class AguiStreamController {

    private static final Logger log = LoggerFactory.getLogger(AguiStreamController.class);
    
    // MCP Standard Route Patterns
    private static final String AGENT_PATTERN = "^/agents/([^/]+)$";
    private static final String AGENTS_STATE_PATTERN = "^/agents/state$";  // Updated to match CopilotKit MCP spec
    private static final String AGENTS_EXECUTE_PATTERN = "^/agents/execute$";
    private static final String ACTION_PATTERN = "^/actions/([^/]+)$";
    private static final String INFO_PATTERN = "^/info$";
    
    @Resource
    private ChatModel chatModel;
    
    private final MessageMapper messageMapper;
    
    public AguiStreamController() {
        this.messageMapper = new MessageMapper();
    }
    
    // 使用 ThreadLocal 存储当前会话的 SseEmitter
    private final ThreadLocal<SseEmitter> currentEmitter = new ThreadLocal<>();

    /**
     * Universal MCP handler for all CopilotKit requests
     * Handles: /info, /agents/{agent}, /agents/state, /agents/execute, /actions/{action}
     */
    @RequestMapping(path = {"/**"}, method = {RequestMethod.GET, RequestMethod.POST})
    @Operation(summary = "MCP CopilotKit Universal Handler", 
            description = "Handles all MCP-compliant CopilotKit requests")
    public Object handleCopilotKitMCPRequest(
            HttpServletRequest request, 
            HttpServletResponse response,
            @RequestBody(required = false) String requestBody) throws Exception {
        
        String path = request.getRequestURI().replaceFirst("/copilotkit", "");
        String method = request.getMethod();
        
        log.info("MCP Request - Path: {}, Method: {}, Body: {}", path, method, requestBody);
        
        // Set CORS headers
        setCorsHeaders(response);
        
        // Route to appropriate handler based on path pattern
        if (path.matches(INFO_PATTERN)) {
            return handleMCPInfo();
        } else if (path.matches(AGENTS_EXECUTE_PATTERN)) {
            // Return SseEmitter wrapped in ResponseEntity for SSE streaming
            SseEmitter emitter = handleMCPAgentsExecute(requestBody, response);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(emitter);
        } else if (path.matches(AGENT_PATTERN)) {
            String agentName = extractAgentName(path);
            return handleMCPAgentExecution(agentName, requestBody, response);
        } else if (path.matches(AGENTS_STATE_PATTERN)) {
            return handleMCPAgentsState(requestBody);
        } else if (path.matches(ACTION_PATTERN)) {
            String actionName = extractActionName(path);
            return handleMCPActionExecution(actionName, requestBody);
        } else if (path.isEmpty() || path.equals("/")) {
            log.error("not route {}" ,path);
            throw new RuntimeException();
        } else {
            // Handle v1 endpoints for backward compatibility
            return handleMCPV1Endpoints(path, method, requestBody, response);
        }
    }
    
    /**
     * MCP Agents Execute Handler - POST /agents/execute
     * This is the main endpoint CopilotKit calls for agent execution
     */
    private SseEmitter handleMCPAgentsExecute(String requestBody, HttpServletResponse response) throws Exception {
        log.info("Executing agents with request: {}", requestBody);
        
        setCorsHeaders(response);
        response.setContentType("text/event-stream");
        
        // This is the primary agent execution endpoint for CopilotKit
        // Use SSE streaming response following AG-UI pattern
        return handleLegacyCopilotKitRequest(requestBody, response);
    }
    
    /**
     * Dedicated agents execute endpoint - POST /agents/execute
     */
    @PostMapping(path = "/agents/execute", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Execute CopilotKit Agents", description = "Main endpoint for CopilotKit agent execution")
    public ResponseEntity<SseEmitter> executeAgents(@RequestBody String requestBody, HttpServletResponse response) throws Exception {
        log.info("Direct agents/execute call with body: {}", requestBody);
        setCorsHeaders(response);
        SseEmitter emitter = handleMCPAgentsExecute(requestBody, response);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }
    
    /**
     * Dedicated agents state endpoint - POST /agents/state
     */
    @PostMapping(path = "/agents/state", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get Agents State", description = "Returns agent state configuration per CopilotKit MCP standards")
    public ResponseEntity<Map<String, Object>> getAgentsState(@RequestBody(required = false) String requestBody) {
        return handleMCPAgentsState(requestBody);
    }

    /**
     * Dedicated info endpoint - GET /info
     */
    @GetMapping(path = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get CopilotKit Info", description = "Returns agents and actions configuration")
    public ResponseEntity<Map<String, Object>> getInfo() {
        return handleMCPInfo();
    }


    private SseEmitter handleLegacyCopilotKitRequest(String inputStr, HttpServletResponse response) throws Exception {

        // 设置响应头 - SSE format following AG-UI pattern
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        setCorsHeaders(response);
        
        // 打印入参用于调试
        log.info("收到请求参数: {}", inputStr);

        // 创建 SseEmitter with long timeout like AG-UI
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        try {
            // 解析JSON并转换字段映射
            com.alibaba.fastjson.JSONObject jsonObject = JSON.parseObject(inputStr);
            RunAgentInput input = createAdaptedRunAgentInput(jsonObject);
            
            // 转换 tools 从 RunAgentInput 到 ReactAgent 认可的 ToolCallback 格式
            List<ToolCallback> toolCallbacks = convertToolsToToolCallbacks(input.tools());
            List<org.springframework.ai.chat.messages.AbstractMessage> springMessages
                    = convertMessagesToSpringMessages(input.messages());
            
            // 创建回调管理器，传入 EventHandler 实例 - Following AG-UI pattern with space prefix
            CallbackManager callbackManager = new CallbackManagerImpl(new EventHandler(event -> {
                try {
                    // AG-UI pattern: add space prefix and send as SSE data
                    String jsonData = " " + JSON.toJSONString(event);
                    emitter.send(SseEmitter.event().data(jsonData).build());
                    log.info("SSE Event sent: {}", JSON.toJSONString(event));
                } catch (IOException e) {
                    log.error("Error sending SSE event", e);
                    emitter.completeWithError(e);
                }
            }));

            // 创建 ReactAgent
            ReactAgent agent = ReactAgent.builder()
                    .name("agui_stream_agent")
                    .model(chatModel)
                    .inputKey("llm_input_messages")
                    .tools(toolCallbacks)
                    .callManager(callbackManager)
                    .build();

            // 获取 CompiledGraph 并设置回调管理器
            CompiledGraph graph = agent.getAndCompileGraph();
            if (graph != null) {
                graph.setCallbackManager(callbackManager);
            }

            // 异步执行 agent，仿照 AG-UI 模式
            CompletableFuture.runAsync(() -> {
                try {
                    // 构建输入参数
                    Map<String, Object> graphInputs = new HashMap<>();
                    graphInputs.put("llm_input_messages", springMessages);
                    graphInputs.put("threadId", input.threadId());
                    graphInputs.put("runId", input.runId());
                    graphInputs.put("tools", input.tools());
                    graphInputs.put("context", input.context());

                    agent.invoke(graphInputs);
                    
                    // Complete the emitter when done
                    emitter.complete();
                    
                } catch (Exception e) {
                    log.error("Error in agent execution", e);
//                    emitter.completeWithError(e);
//

                }
            });
            
        } catch (Exception e) {
            log.error("Error in streaming response setup", e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }

    /**
     * MCP Info Endpoint - GET /info
     * Returns agents and actions information per MCP standards
     */
    private ResponseEntity<Map<String, Object>> handleMCPInfo() {
        try {
            Map<String, Object> info = new HashMap<>();

            // 添加agents信息
            Map<String, Object> agent = new HashMap<>();
            agent.put("name", "ai_researcher");
            agent.put("description", "AGUI Stream Agent for handling chat sessions");
            agent.put("type", "react");

            List<Map<String, Object>> agents = new ArrayList<>();
            agents.add(agent);
            info.put("agents", agents);

            // 添加actions信息 - CopilotKit format
            List<Map<String, Object>> actions = new ArrayList<>();

            // Simple test action without parameters
            Map<String, Object> testAction = new HashMap<>();
            testAction.put("name", "test_action");
            testAction.put("description", "Simple test action");
            testAction.put("actionParameters", new ArrayList<>()); // Empty parameters array
            actions.add(testAction);

            Map<String, Object> weatherAction = new HashMap<>();
            weatherAction.put("name", "weather_tool");
            weatherAction.put("description", "Get weather information for a specified city");
            
            // CopilotKit expects actionParameters as an array of parameter objects
            List<Map<String, Object>> actionParameters = new ArrayList<>();
            
            Map<String, Object> cityParam = new HashMap<>();
            cityParam.put("name", "city");
            cityParam.put("type", "string");
            cityParam.put("description", "City name");
            cityParam.put("required", true);
            actionParameters.add(cityParam);
            
            Map<String, Object> timestampParam = new HashMap<>();
            timestampParam.put("name", "currentTimestamp");
            timestampParam.put("type", "string");
            timestampParam.put("description", "Current timestamp");
            timestampParam.put("required", true);
            actionParameters.add(timestampParam);
            
            weatherAction.put("actionParameters", actionParameters);
            actions.add(weatherAction);

            info.put("actions", actions);

            // 添加其他必要信息
            info.put("version", "1.0.0");
            info.put("status", "active");
            
            log.info("MCP Info Response: {}", JSON.toJSONString(info));

            return ResponseEntity.ok(info);

        } catch (Exception e) {
            log.error("Error getting CopilotKit info", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get CopilotKit info");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("agents", new ArrayList<>());
            errorResponse.put("actions", new ArrayList<>());
            errorResponse.put("status", "error");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * MCP Agent Execution Handler - POST /agents/{agent}
     */
    private Object handleMCPAgentExecution(String agentName, String requestBody, HttpServletResponse response) throws Exception {
        log.info("Executing agent: {} with request: {}", agentName, requestBody);
        
        if ("ai_researcher".equals(agentName)) {
            // Handle streaming response for ai_researcher agent - return SseEmitter wrapped in ResponseEntity
            SseEmitter emitter = handleLegacyCopilotKitRequest(requestBody, response);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(emitter);
        } else {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Agent not found");
            error.put("agentName", agentName);
            error.put("availableAgents", List.of("ai_researcher"));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    /**
     * MCP Agents State Handler - POST /agents/state
     * Returns agent state information per CopilotKit MCP standards
     */
    private ResponseEntity<Map<String, Object>> handleMCPAgentsState(String requestBody) {
        try {
            log.info("Loading agents state with request: {}", requestBody);
            
            Map<String, Object> stateResponse = new HashMap<>();
            
            // Parse request to extract threadId and agentName
            String threadId = "default-thread-" + System.currentTimeMillis();
            String agentName = "ai_researcher"; // Default agent
            
            if (requestBody != null && !requestBody.trim().isEmpty()) {
                try {
                    com.alibaba.fastjson.JSONObject request = JSON.parseObject(requestBody);
                    String requestThreadId = request.getString("threadId");
                    String requestAgentName = request.getString("name");
                    
                    if (requestThreadId != null && !requestThreadId.trim().isEmpty()) {
                        threadId = requestThreadId;
                    }
                    if (requestAgentName != null && !requestAgentName.trim().isEmpty()) {
                        agentName = requestAgentName;
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse request body, using defaults", e);
                }
            }
            
            // Build CopilotKit MCP-compliant state response
            stateResponse.put("threadId", threadId);
            stateResponse.put("threadExists", true);
            stateResponse.put("state", JSON.toJSONString(Map.of(
                "agentName", agentName,
                "status", "active",
                "lastActivity", new java.util.Date().toString(),
                "mode", "agent_lock"
            )));
            stateResponse.put("messages", JSON.toJSONString(new ArrayList<>()));
            
            log.info("MCP Agents State Response: {}", JSON.toJSONString(stateResponse));
            
            return ResponseEntity.ok(stateResponse);
            
        } catch (Exception e) {
            log.error("Error loading agents state", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to load agents state");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("threadId", "error-thread-" + System.currentTimeMillis());
            errorResponse.put("threadExists", false);
            errorResponse.put("state", JSON.toJSONString(Map.of()));
            errorResponse.put("messages", JSON.toJSONString(new ArrayList<>()));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * MCP Action Execution Handler - POST /actions/{action}
     */
    private ResponseEntity<Map<String, Object>> handleMCPActionExecution(String actionName, String requestBody) {
        try {
            log.info("Executing action: {} with request: {}", actionName, requestBody);
            
            Map<String, Object> actionResponse = new HashMap<>();
            
            // Handle different action types
            switch (actionName) {
                case "weather_tool":
                    String result = executeWeatherTool(requestBody);
                    actionResponse.put("result", result);
                    actionResponse.put("status", "success");
                    break;
                default:
                    actionResponse.put("error", "Action not found");
                    actionResponse.put("actionName", actionName);
                    actionResponse.put("availableActions", List.of("weather_tool"));
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(actionResponse);
            }
            
            actionResponse.put("actionName", actionName);
            actionResponse.put("timestamp", new java.util.Date().toString());
            
            return ResponseEntity.ok(actionResponse);
            
        } catch (Exception e) {
            log.error("Error executing action: " + actionName, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to execute action");
            errorResponse.put("actionName", actionName);
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Handle v1 endpoints for backward compatibility
     */
    private ResponseEntity<Map<String, Object>> handleMCPV1Endpoints(String path, String method, String requestBody, HttpServletResponse response) {
        setCorsHeaders(response);
        
        Map<String, Object> v1Response = new HashMap<>();
        v1Response.put("version", "v1");
        v1Response.put("path", path);
        v1Response.put("method", method);
        v1Response.put("message", "MCP v1 compatibility layer");
        v1Response.put("timestamp", new java.util.Date().toString());
        
        return ResponseEntity.ok(v1Response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping(path = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("service", "CopilotKit MCP Controller");
        health.put("mode", "agent_lock");
        health.put("primaryAgent", "ai_researcher");
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
     * Extract agent name from path
     */
    private String extractAgentName(String path) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(AGENT_PATTERN);
        java.util.regex.Matcher matcher = pattern.matcher(path);
        return matcher.matches() ? matcher.group(1) : "unknown";
    }
    
    /**
     * Extract action name from path
     */
    private String extractActionName(String path) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(ACTION_PATTERN);
        java.util.regex.Matcher matcher = pattern.matcher(path);
        return matcher.matches() ? matcher.group(1) : "unknown";
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
            ToolCallback weatherToolCallback = ToolCallbacks.from(new WeatherTool())[0];
            return List.of(weatherToolCallback);
        }

        List<ToolCallback> toolCallbacks = new ArrayList<>();
        Set<String> registeredToolNames = new HashSet<>(); // 用于防止重复注册

        for (Tool tool : tools) {
            // 检查是否已经注册过相同名称的工具
            if (registeredToolNames.contains(tool.name())) {
                log.warn("工具 {} 已经注册，跳过重复注册", tool.name());
                continue;
            }
            
            // 为每个 Tool 创建一个 FunctionToolCallback
            FunctionToolCallback toolCallback = FunctionToolCallback.builder(tool.name(), (String input) -> {
                        // 这里可以实现具体的工具执行逻辑
                        log.info("工具 {} 被调用，参数: {}", tool.name(), input);
                        return "Tool " + tool.name() + " executed with input: " + input;
                    })
                    .description(tool.description())
                    .inputType(String.class)
                    .build();

            toolCallbacks.add(toolCallback);
            registeredToolNames.add(tool.name());
        }
        
        // 只在没有weather_tool时才添加默认的weather tool
        if (!registeredToolNames.contains("weather_tool")) {
            ToolCallback weatherToolCallback = ToolCallbacks.from(new WeatherTool())[0];
            toolCallbacks.add(weatherToolCallback);
            registeredToolNames.add("weather_tool");
        }
        
        log.info("注册的工具: {}", registeredToolNames);
        return toolCallbacks;
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
            return String.format("{\"city\": \"%s\", \"temperature\": -50, \"time\": \"%s\"}", city, currentTimestamp);
        }
    }
}