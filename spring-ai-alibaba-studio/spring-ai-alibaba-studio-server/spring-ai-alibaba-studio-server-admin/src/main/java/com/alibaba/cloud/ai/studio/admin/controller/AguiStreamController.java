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

import com.alibaba.cloud.ai.dashscope.event.agent.*;
import com.alibaba.cloud.ai.dashscope.event.event.*;
import com.alibaba.cloud.ai.dashscope.event.message.*;
import com.alibaba.cloud.ai.dashscope.event.type.EventType;
import com.alibaba.cloud.ai.graph.CallbackManager;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AGUI Stream Controller for handling real-time AI agent interactions.
 * <p>
 * This controller provides streaming endpoints for AI agent conversations,
 * integrating AGUI framework with Spring AI ChatClient for seamless
 * real-time communication.
 * </p>
 *
 * @author AI Studio Team
 */
@Slf4j
@RestController
@Tag(name = "agui-stream", description = "AGUI Stream API for real-time AI interactions")
@RequestMapping("/console/v1/agui-stream")
@RequiredArgsConstructor
public class AguiStreamController {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final CallbackManager callbackManager;
    
    // 用于跟踪活跃的流式连接
    private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();
    private final AtomicInteger connectionCounter = new AtomicInteger(0);

    /**
     * 启动流式对话会话
     *
     * @param request 对话请求参数
     * @return SSE Emitter 用于流式响应
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Start streaming chat session", description = "Initiates a real-time streaming chat session with AI agent")
    public SseEmitter startStreamingChat(@RequestBody StreamChatRequest request) {
        String sessionId = "session_" + connectionCounter.incrementAndGet();
        SseEmitter emitter = new SseEmitter(0L); // 无超时
        
        // 注册 emitter
        activeEmitters.put(sessionId, emitter);
        
        // 设置完成和错误回调
        emitter.onCompletion(() -> {
            log.info("Streaming session {} completed", sessionId);
            activeEmitters.remove(sessionId);
        });
        
        emitter.onError(throwable -> {
            log.error("Error in streaming session {}: {}", sessionId, throwable.getMessage());
            activeEmitters.remove(sessionId);
        });
        
        // 启动异步处理
        CompletableFuture.runAsync(() -> {
            try {
                processStreamingChat(sessionId, request, emitter);
            } catch (Exception e) {
                log.error("Error processing streaming chat for session {}: {}", sessionId, e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Result.error("Stream processing error: " + e.getMessage(), ErrorCode.SYSTEM_ERROR)));
                    emitter.complete();
                } catch (IOException ex) {
                    log.error("Failed to send error event", ex);
                }
            }
        });
        
        return emitter;
    }

    /**
     * 处理流式对话的核心逻辑
     */
    private void processStreamingChat(String sessionId, StreamChatRequest request, SseEmitter emitter) throws IOException, GraphStateException {
        log.info("Starting streaming chat session: {}", sessionId);
        
        // 创建 AGUI 消息列表
        List<BaseMessage> aguiMessages = convertToAguiMessages(request.getMessages());
        
        // 创建工具列表
        List<com.alibaba.cloud.ai.dashscope.event.tool.Tool> aguiTools = convertToAguiTools(request.getTools());
        
        // 创建运行参数
        RunAgentParameters parameters = RunAgentParameters.builder()
            .runId(request.getRunId())
            .tools(aguiTools)
            .build();
        
        // 创建自定义订阅者
        AgentSubscriber subscriber = createStreamingSubscriber(emitter, sessionId);
        
        // 创建并运行 ReactAgent
        ReactAgent agent = ReactAgent.builder()
            .name("agui_stream_agent")
            .model(chatClient)
            .tools(convertToSpringTools(aguiTools))
            .build();
        
        // 设置 CallbackManager 到 CompiledGraph
        CompiledGraph graph = agent.getAndCompileGraph();
        if (graph != null) {
            graph.setCallbackManager(callbackManager);
        }
        
        // 发送会话开始事件
        emitter.send(SseEmitter.event()
            .name("session_started")
            .data(Result.success(Map.of("sessionId", sessionId, "status", "started"))));
        
        // 运行代理
        try {
            Map<String, Object> input = Map.of("messages", aguiMessages);
            Optional<OverAllState> result = agent.invoke(input);
            
            if (result.isPresent()) {
                log.info("Agent execution completed for session: {}", sessionId);
                emitter.send(SseEmitter.event()
                    .name("session_completed")
                    .data(Result.success(Map.of("sessionId", sessionId, "status", "completed"))));
            } else {
                log.warn("Agent execution returned no result for session: {}", sessionId);
                emitter.send(SseEmitter.event()
                    .name("session_completed")
                    .data(Result.success(Map.of("sessionId", sessionId, "status", "completed_no_result"))));
            }
        } catch (Exception e) {
            log.error("Agent execution failed for session {}: {}", sessionId, e.getMessage());
            emitter.send(SseEmitter.event()
                .name("error")
                .data(Result.error("Agent execution failed: " + e.getMessage(), ErrorCode.SYSTEM_ERROR)));
        } finally {
            emitter.complete();
        }
    }

    /**
     * 创建流式订阅者
     */
    private AgentSubscriber createStreamingSubscriber(SseEmitter emitter, String sessionId) {
        return new AgentSubscriber() {
            @Override
            public void onRunInitialized(AgentSubscriberParams params) {
                try {
                    emitter.send(SseEmitter.event()
                        .name("run_initialized")
                        .data(Result.success(Map.of("sessionId", sessionId, "status", "initialized"))));
                } catch (IOException e) {
                    log.error("Failed to send run initialized event", e);
                }
            }

            @Override
            public void onEvent(BaseEvent event) {
                try {
                    // 根据事件类型发送相应的 SSE 事件
                    String eventName = event.getType().name().toLowerCase();
                    emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(Result.success(event)));
                } catch (IOException e) {
                    log.error("Failed to send event: {}", event.getType(), e);
                }
            }

            @Override
            public void onRunStartedEvent(RunStartedEvent event) {
                try {
                    emitter.send(SseEmitter.event()
                        .name("run_started")
                        .data(Result.success(event)));
                } catch (IOException e) {
                    log.error("Failed to send run started event", e);
                }
            }

            @Override
            public void onRunFinishedEvent(RunFinishedEvent event) {
                try {
                    emitter.send(SseEmitter.event()
                        .name("run_finished")
                        .data(Result.success(event)));
                } catch (IOException e) {
                    log.error("Failed to send run finished event", e);
                }
            }

            @Override
            public void onRunErrorEvent(RunErrorEvent event) {
                try {
                    emitter.send(SseEmitter.event()
                        .name("run_error")
                        .data(Result.error(event.getError(), ErrorCode.SYSTEM_ERROR)));
                } catch (IOException e) {
                    log.error("Failed to send run error event", e);
                }
            }

            @Override
            public void onRunFailed(AgentSubscriberParams params, Throwable error) {
                try {
                    emitter.send(SseEmitter.event()
                        .name("run_failed")
                        .data(Result.error("Run failed: " + error.getMessage(), ErrorCode.SYSTEM_ERROR)));
                } catch (IOException e) {
                    log.error("Failed to send run failed event", e);
                }
            }

            @Override
            public void onRunFinalized(AgentSubscriberParams params) {
                try {
                    emitter.send(SseEmitter.event()
                        .name("run_finalized")
                        .data(Result.success(Map.of("sessionId", sessionId, "status", "finalized"))));
                } catch (IOException e) {
                    log.error("Failed to send run finalized event", e);
                }
            }
        };
    }

    /**
     * 转换 Spring AI 消息为 AGUI 消息
     */
    private List<BaseMessage> convertToAguiMessages(List<String> messages) {
        List<BaseMessage> aguiMessages = new ArrayList<>();
        
        for (String message : messages) {
            // 创建用户消息
            UserMessage aguiMessage = new UserMessage();
            aguiMessage.setContent(message);
            aguiMessage.setId(UUID.randomUUID().toString());
            
            aguiMessages.add(aguiMessage);
        }
        
        return aguiMessages;
    }

    /**
     * 转换工具为 AGUI 工具格式
     */
    private List<com.alibaba.cloud.ai.dashscope.event.tool.Tool> convertToAguiTools(List<ToolDefinition> tools) {
        List<com.alibaba.cloud.ai.dashscope.event.tool.Tool> aguiTools = new ArrayList<>();
        
        for (ToolDefinition toolDef : tools) {
            // 创建 ToolParameters
            com.alibaba.cloud.ai.dashscope.event.tool.Tool.ToolParameters parameters = 
                new com.alibaba.cloud.ai.dashscope.event.tool.Tool.ToolParameters(
                    "object", 
                    Map.of(), 
                    List.of()
                );
            
            // 创建 Tool record
            com.alibaba.cloud.ai.dashscope.event.tool.Tool aguiTool = 
                new com.alibaba.cloud.ai.dashscope.event.tool.Tool(
                    toolDef.getName(),
                    toolDef.getDescription(),
                    parameters
                );
            
            aguiTools.add(aguiTool);
        }
        
        return aguiTools;
    }
    
    /**
     * 转换 AGUI 工具为 Spring AI 工具回调
     */
    private List<org.springframework.ai.tool.ToolCallback> convertToSpringTools(List<com.alibaba.cloud.ai.dashscope.event.tool.Tool> aguiTools) {
        List<org.springframework.ai.tool.ToolCallback> springTools = new ArrayList<>();
        
        for (com.alibaba.cloud.ai.dashscope.event.tool.Tool aguiTool : aguiTools) {
            org.springframework.ai.tool.ToolCallback springTool = new org.springframework.ai.tool.ToolCallback() {
                @Override
                public String call(String arguments, Object context) {
                    // 模拟工具执行
                    return "Tool " + aguiTool.getName() + " executed with arguments: " + arguments;
                }
                
                @Override
                public Object getToolDefinition() {
                    return Map.of(
                        "name", aguiTool.getName(),
                        "description", aguiTool.getDescription(),
                        "inputSchema", aguiTool.getInputSchema()
                    );
                }
            };
            
            springTools.add(springTool);
        }
        
        return springTools;
    }

    /**
     * 获取活跃会话列表
     */
    @GetMapping("/sessions/active")
    @Operation(summary = "Get active streaming sessions", description = "Retrieves list of currently active streaming sessions")
    public Result<List<SessionInfo>> getActiveSessions() {
        List<SessionInfo> sessions = activeEmitters.entrySet().stream()
            .map(entry -> new SessionInfo(entry.getKey(), "active"))
            .toList();
        
        return Result.success(sessions);
    }

    /**
     * 关闭指定的流式会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Close streaming session", description = "Closes a specific streaming session")
    public Result<String> closeSession(@PathVariable String sessionId) {
        SseEmitter emitter = activeEmitters.remove(sessionId);
        if (emitter != null) {
            emitter.complete();
            log.info("Session {} closed successfully", sessionId);
            return Result.success("Session closed successfully");
        } else {
            return Result.error("Session not found: " + sessionId, ErrorCode.SYSTEM_ERROR);
        }
    }

    /**
     * 关闭所有活跃的流式会话
     */
    @DeleteMapping("/sessions/all")
    @Operation(summary = "Close all streaming sessions", description = "Closes all currently active streaming sessions")
    public Result<String> closeAllSessions() {
        int count = activeEmitters.size();
        activeEmitters.values().forEach(SseEmitter::complete);
        activeEmitters.clear();
        
        log.info("Closed {} active streaming sessions", count);
        return Result.success("Closed " + count + " active sessions");
    }

    // 内部类定义

    /**
     * 流式对话请求
     */
    @Data
    public static class StreamChatRequest {
        @Parameter(description = "对话消息列表")
        private List<String> messages;
        
        @Parameter(description = "可用工具列表")
        private List<ToolDefinition> tools;
        
        @Parameter(description = "运行ID")
        private String runId;
        
        @Parameter(description = "系统提示词")
        private String systemPrompt;
    }

    /**
     * 工具定义
     */
    @Data
    public static class ToolDefinition {
        @Parameter(description = "工具名称")
        private String name;
        
        @Parameter(description = "工具描述")
        private String description;
        
        @Parameter(description = "输入模式")
        private Object inputSchema;
    }

    /**
     * 会话信息
     */
    @Data
    public static class SessionInfo {
        private String sessionId;
        private String status;
        
        public SessionInfo(String sessionId, String status) {
            this.sessionId = sessionId;
            this.status = status;
        }
    }

    /**
     * Spring AI 代理实现，集成 AGUI 框架
     */
    private static class SpringAIAgent implements Agent {
        
        private final String agentId;
        private final ChatClient chatClient;
        private final List<BaseMessage> messages;
        private final ObjectMapper objectMapper;
        private final MessageMapper messageMapper;
        private final ToolMapper toolMapper;

        public SpringAIAgent(String agentId, ChatClient chatClient, List<BaseMessage> messages) {
            this.agentId = agentId;
            this.chatClient = chatClient;
            this.messages = messages;
            this.objectMapper = new ObjectMapper();
            this.messageMapper = new MessageMapper();
            this.toolMapper = new ToolMapper(this.objectMapper);
        }

        @Override
        public CompletableFuture<Void> runAgent(RunAgentParameters parameters, AgentSubscriber subscriber) {
            return CompletableFuture.runAsync(() -> {
                try {
                    run(parameters, subscriber);
                } catch (Exception e) {
                    log.error("Error running agent: {}", e.getMessage());
                    // 创建空的运行输入
                    RunAgentInput emptyInput = new RunAgentInput(
                        "empty", "empty", null, messages, 
                        Collections.emptyList(), Collections.emptyList(), null
                    );
                    subscriber.onRunFailed(new AgentSubscriberParams(messages, null, this, emptyInput), e);
                }
            });
        }

        /**
         * 执行代理逻辑
         */
        private void run(RunAgentParameters parameters, AgentSubscriber subscriber) {
            // 创建空的运行输入
            RunAgentInput emptyInput = new RunAgentInput(
                "empty", "empty", null, messages, 
                Collections.emptyList(), Collections.emptyList(), null
            );
            
            // 通知运行初始化
            subscriber.onRunInitialized(new AgentSubscriberParams(messages, null, this, emptyInput));

            // 转换消息格式
            List<Message> springMessages = messages.stream()
                .map(messageMapper::toSpringMessage)
                .toList();

            // 创建工具回调
            List<ToolCallback> toolCallbacks = parameters.getTools().stream()
                .map(tool -> toolMapper.toSpringTool(tool, UUID.randomUUID().toString(), event -> {}))
                .toList();

            // 构建提示词
            Prompt prompt = Prompt.builder()
                .messages(springMessages.toArray(new AbstractMessage[0]))
                .build();

            // 执行流式对话
            chatClient.prompt(prompt)
                .toolCallbacks(toolCallbacks)
                .stream()
                .chatResponse()
                .subscribe(
                    // 处理响应事件
                    evt -> {
                        if (evt.getResult().getOutput().getText() != null) {
                            // 发送文本内容事件
                            TextMessageContentEvent contentEvent = new TextMessageContentEvent();
                            contentEvent.setType(EventType.TEXT_MESSAGE_CONTENT);
                            contentEvent.setMessageId(UUID.randomUUID().toString());
                            contentEvent.setDelta(evt.getResult().getOutput().getText());
                            subscriber.onEvent(contentEvent);
                        }
                    },
                    // 处理错误
                    err -> {
                        log.error("Chat stream error: {}", err.getMessage());
                        RunErrorEvent errorEvent = new RunErrorEvent();
                        errorEvent.setType(EventType.RUN_ERROR);
                        errorEvent.setError(err.getMessage());
                        subscriber.onRunErrorEvent(errorEvent);
                    },
                    // 处理完成
                    () -> {
                        log.info("Chat stream completed for agent: {}", agentId);
                        subscriber.onRunFinalized(new AgentSubscriberParams(messages, null, this, emptyInput));
                    }
                );
        }
    }

    /**
     * 消息映射器
     */
    private static class MessageMapper {
        public Message toSpringMessage(BaseMessage aguiMessage) {
            return new UserMessage(aguiMessage.getContent());
        }
    }

    /**
     * 工具映射器
     */
    private static class ToolMapper {
        private final ObjectMapper objectMapper;

        public ToolMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        public ToolCallback toSpringTool(com.alibaba.cloud.ai.dashscope.event.tool.Tool aguiTool, String messageId, 
                                       java.util.function.Consumer<BaseEvent> eventCollector) {
            return new ToolCallback() {
                @Override
                public String call(String arguments, Object context) {
                    try {
                        // 发送工具调用开始事件
                        ToolCallStartEvent startEvent = new ToolCallStartEvent();
                        startEvent.setType(EventType.TOOL_CALL_START);
                        startEvent.setToolCallId(messageId);
                        startEvent.setToolName(aguiTool.getName());
                        eventCollector.accept(startEvent);

                        // 模拟工具执行
                        String result = "Tool " + aguiTool.getName() + " executed with arguments: " + arguments;

                        // 发送工具调用结果事件
                        ToolCallResultEvent resultEvent = new ToolCallResultEvent();
                        resultEvent.setType(EventType.TOOL_CALL_RESULT);
                        resultEvent.setToolCallId(messageId);
                        resultEvent.setToolName(aguiTool.getName());
                        resultEvent.setResult(result);
                        eventCollector.accept(resultEvent);

                        // 发送工具调用结束事件
                        ToolCallEndEvent endEvent = new ToolCallEndEvent();
                        endEvent.setType(EventType.TOOL_CALL_END);
                        endEvent.setToolCallId(messageId);
                        endEvent.setToolName(aguiTool.getName());
                        eventCollector.accept(endEvent);

                        return result;
                    } catch (Exception e) {
                        log.error("Error executing tool {}: {}", aguiTool.getName(), e.getMessage());
                        return "Error: " + e.getMessage();
                    }
                }

                @Override
                public Object getToolDefinition() {
                    return aguiTool;
                }
            };
        }
    }
}
