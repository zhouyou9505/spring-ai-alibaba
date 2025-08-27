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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
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
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/agui")
@Tag(name = "AGUI Stream Controller", description = "AGUI streaming chat controller")
public class AguiStreamController {

    private static final Logger log = LoggerFactory.getLogger(AguiStreamController.class);
    @Resource
    private ChatModel chatModel; // 注入 ChatModel

    private final MessageMapper messageMapper;

    public AguiStreamController() {
        this.messageMapper = new MessageMapper();
    }

    // 使用 ThreadLocal 存储当前会话的 SseEmitter
    private final ThreadLocal<SseEmitter> currentEmitter = new ThreadLocal<>();

    @PostMapping(path = "/copilotkit", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Start streaming chat session",
            description = "Initiates a real-time streaming chat session with AI agent")
    public SseEmitter copilotKit(@RequestBody String inputStr) throws Exception {

        // 打印入参用于调试
        System.out.println("收到请求参数: " + inputStr);

        // 解析JSON并转换字段映射
        com.alibaba.fastjson.JSONObject jsonObject = JSON.parseObject(inputStr);

        // 创建适配的RunAgentInput
        RunAgentInput input = createAdaptedRunAgentInput(jsonObject);

        // 创建 SseEmitter
        SseEmitter emitter = new SseEmitter(0L); // 无超时
        String threadId = input.threadId();

        // 设置当前线程的 SseEmitter
        currentEmitter.set(emitter);

        // 设置 SseEmitter 的回调，清理 ThreadLocal
        emitter.onCompletion(() -> {
            currentEmitter.remove();
        });

        emitter.onError(throwable -> {
            currentEmitter.remove();
        });

        emitter.onTimeout(() -> {
            currentEmitter.remove();
        });

        // 转换 tools 从 RunAgentInput 到 ReactAgent 认可的 ToolCallback 格式
        List<ToolCallback> toolCallbacks = convertToolsToToolCallbacks(input.tools());

        // 转换 messages 从 RunAgentInput 到 ReactAgent 认可的 Spring AI Message 格式
        List<org.springframework.ai.chat.messages.AbstractMessage> springMessages
                = convertMessagesToSpringMessages(input.messages());

        // 创建 ReactAgent - 学习 ReactAgentHookTest.java 的完整初始化方式
        ReactAgent agent = ReactAgent.builder()
                .name("agui_stream_agent")
                .model(chatModel)
                .inputKey("llm_input_messages") // 设置输入键
                .tools(toolCallbacks)
                .build();

        // 创建回调管理器，传入 EventHandler 实例
        CallbackManager callbackManager = new CallbackManagerImpl(new EventHandler(event -> {
            if (emitter == null) {
                return;
            }
            try {
                log.info(JSON.toJSONString(event));
                emitter.send(SseEmitter.event()
                        .name(event.getClass().getSimpleName())
                        .data(event));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));

        // 获取 CompiledGraph 并设置回调管理器
        CompiledGraph graph = agent.getAndCompileGraph();
        if (graph != null) {
            graph.setCallbackManager(callbackManager);
        }

        // 异步启动 ReactAgent 执行
        CompletableFuture.runAsync(() -> {
            try {
                // 构建输入参数 - 按照 ReactAgentHookTest.java 的方式
                Map<String, Object> graphInputs = new HashMap<>();
                graphInputs.put("llm_input_messages", springMessages); // 使用转换后的 Spring AI messages
                graphInputs.put("threadId", input.threadId());
                graphInputs.put("runId", input.runId());
                graphInputs.put("tools", input.tools());
                graphInputs.put("context", input.context());

                agent.invoke(graphInputs);

            } catch (Exception e) {
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    // 静默处理
                }
            } finally {
                // 确保清理 ThreadLocal
                currentEmitter.remove();
            }
        });

        return emitter;
    }

    /**
     * 转换 RunAgentInput 的 tools 到 ReactAgent 认可的 ToolCallback 格式
     */
    private List<ToolCallback> convertToolsToToolCallbacks(List<Tool> tools) {
        if (tools == null || tools.isEmpty()) {
            return List.of();
        }

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (Tool tool : tools) {
            // 为每个 Tool 创建一个 FunctionToolCallback
            FunctionToolCallback toolCallback = FunctionToolCallback.builder(tool.name(), (String input) -> {
                        // 这里可以实现具体的工具执行逻辑
                        // 暂时返回一个占位符响应
                        return "Tool " + tool.name() + " executed with input: " + input;
                    })
                    .description(tool.description())
                    .inputType(String.class)
                    .build();

            toolCallbacks.add(toolCallback);
        }

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
            com.alibaba.fastjson.JSONObject schema = JSON.parseObject(jsonSchema);
            String type = schema.getString("type");

            // 解析properties
            Map<String, Tool.ToolProperty> properties = new HashMap<>();
            com.alibaba.fastjson.JSONObject props = schema.getJSONObject("properties");
            if (props != null) {
                for (String key : props.keySet()) {
                    com.alibaba.fastjson.JSONObject prop = props.getJSONObject(key);
                    String propType = prop.getString("type");
                    String propDesc = prop.getString("description");
                    if (propDesc == null) propDesc = "";

                    properties.put(key, new Tool.ToolProperty(propType, propDesc));
                }
            }

            // 解析required
            List<String> required = new ArrayList<>();
            com.alibaba.fastjson.JSONArray reqArray = schema.getJSONArray("required");
            if (reqArray != null) {
                for (int i = 0; i < reqArray.size(); i++) {
                    required.add(reqArray.getString(i));
                }
            }

            return new Tool.ToolParameters(type, properties, required);
        } catch (Exception e) {
            System.err.println("解析JSON Schema失败: " + e.getMessage());
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
                message = new com.alibaba.cloud.ai.graph.event.message.AssistantMessage(id, content, "",
                        JSON.parseArray(msgObj.getJSONArray("toolCall").toJSONString(), ToolCall.class));
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


}
