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

import com.alibaba.cloud.ai.graph.event.manager.CallbackManager;
import com.alibaba.cloud.ai.graph.event.manager.CallbackManagerImpl;
import com.alibaba.cloud.ai.graph.event.agent.RunAgentInput;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.event.manager.EventHandler;
import com.alibaba.cloud.ai.graph.event.message.BaseMessage;
import com.alibaba.cloud.ai.graph.event.tool.Tool;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.ai.support.ToolCallbacks;
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
@RequiredArgsConstructor
public class AguiStreamController {
    
    private final ObjectMapper objectMapper;
    private final ChatModel chatModel; // 注入 ChatModel
    
    // 使用 ThreadLocal 存储当前会话的 SseEmitter
    private final ThreadLocal<SseEmitter> currentEmitter = new ThreadLocal<>();

    @PostMapping(path = "/copilotkit", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Start streaming chat session",
            description = "Initiates a real-time streaming chat session with AI agent")
    public SseEmitter copilotKit(@RequestBody RunAgentInput input) throws Exception {
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
        List<org.springframework.ai.chat.messages.Message> springMessages = convertMessagesToSpringMessages(input.messages());
        
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

            // 通过 SseEmitter 发送事件给前端
            try {
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
     * 转换 RunAgentInput 的 messages 到 React AI 认可的 Message 格式
     */
    private List<org.springframework.ai.chat.messages.Message> convertMessagesToSpringMessages(List<BaseMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        
        List<org.springframework.ai.chat.messages.Message> springMessages = new ArrayList<>();
        
        for (BaseMessage message : messages) {
            String role = message.getRole();
            String content = message.getContent();
            
            org.springframework.ai.chat.messages.Message springMessage = switch (role.toLowerCase()) {
                case "user" -> new org.springframework.ai.chat.messages.UserMessage(content);
                case "assistant" -> new org.springframework.ai.chat.messages.AssistantMessage(content);
                case "system" -> new org.springframework.ai.chat.messages.SystemMessage(content);
                default -> new org.springframework.ai.chat.messages.UserMessage(content); // 默认作为用户消息
            };
            
            springMessages.add(springMessage);
        }
        
        return springMessages;
    }
}
