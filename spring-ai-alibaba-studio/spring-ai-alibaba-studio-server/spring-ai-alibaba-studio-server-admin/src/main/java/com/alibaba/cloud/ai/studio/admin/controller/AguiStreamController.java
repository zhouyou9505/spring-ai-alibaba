package com.alibaba.cloud.ai.studio.admin.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

/**
 * AG-UI Stream Controller - Standard Implementation
 * 
 * Provides SSE streaming endpoint for AG-UI protocol events following the official specification.
 * Implements the standard AG-UI event flow: RUN_STARTED -> TEXT_MESSAGE_CHUNK -> TOOL_CALL_CHUNK -> RUN_FINISHED
 * 
 * @see <a href="https://docs.ag-ui.com/llms-full.txt">AG-UI Documentation</a>
 */
@RestController
@RequestMapping("/api/agui")
@CrossOrigin(origins = {"localhost", "http://localhost:8000"})
public class AguiStreamController {

    /**
     * AG-UI Stream Endpoint - Standard Implementation
     * Streams AG-UI protocol events via Server-Sent Events following the official specification
     * 
     * @param question The user's question
     * @param filter The event filter type (ALL, MESSAGE, TOOL, STATE, LIFECYCLE)
     * @param limit The maximum number of events to send
     * @return SseEmitter for streaming AG-UI protocol events
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam String question,
            @RequestParam(defaultValue = "ALL") String filter,
            @RequestParam(defaultValue = "120") int limit) {
        
        SseEmitter emitter = new SseEmitter(0L);
        
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                String threadId = "thread-" + UUID.randomUUID().toString().substring(0, 8);
                String runId = "run-" + UUID.randomUUID().toString().substring(0, 8);
                String messageId = "msg-" + UUID.randomUUID().toString().substring(0, 8);
                
                // Helper function to send events with 2-second delay (faster for better UX)
                BiConsumer<String, Map<String, Object>> send = (type, extra) -> {
                    try {
                        Map<String, Object> evt = new LinkedHashMap<>();
                        evt.put("type", type);
                        evt.put("timestamp", System.currentTimeMillis());
                        evt.put("threadId", threadId);
                        evt.put("runId", runId);
                        if (extra != null) {
                            evt.putAll(extra);
                        }
                        
                        // Send event directly without wrapping in "agui" name (AG-UI standard)
                        emitter.send(SseEmitter.event().data(evt));
                        Thread.sleep(2000); // 2 seconds delay between events for better UX
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };

                // Step 1: Setup - Emit RUN_STARTED (following AG-UI standard)
                if ("ALL".equals(filter) || "LIFECYCLE".equals(filter)) {
                    send.accept("RUN_STARTED", Map.of());
                }
                
                // Step 2: Request - Start processing the user's question
                if ("ALL".equals(filter) || "LIFECYCLE".equals(filter)) {
                    send.accept("STEP_STARTED", Map.of("stepName", "process_question"));
                }
                
                // Step 3: Streaming - Emit TEXT_MESSAGE_CHUNK events (AG-UI standard)
                if ("ALL".equals(filter) || "MESSAGE".equals(filter)) {
                    // Start text message
                    send.accept("TEXT_MESSAGE_START", Map.of(
                        "messageId", messageId,
                        "role", "assistant"
                    ));
                    
                    // Generate response based on question
                    String response = generateResponse(question);
                    
                    // Split response into chunks and emit TEXT_MESSAGE_CHUNK (AG-UI standard)
                    String[] chunks = response.split("(?<=\\G.{8})"); // Split every 8 characters for realistic streaming
                    
                    for (String chunk : chunks) {
                        if (!chunk.trim().isEmpty()) {
                            send.accept("TEXT_MESSAGE_CHUNK", Map.of(
                                "messageId", messageId,
                                "delta", chunk
                            ));
                        }
                    }
                    
                    // End text message
                    send.accept("TEXT_MESSAGE_END", Map.of("messageId", messageId));
                }
                
                // Step 4: Tool Calls - Emit TOOL_CALL_CHUNK events (AG-UI standard)
                if ("ALL".equals(filter) || "TOOL".equals(filter)) {
                    String toolCallId = "tool-" + UUID.randomUUID().toString().substring(0, 8);
                    
                    // Start tool call
                    send.accept("TOOL_CALL_START", Map.of(
                        "toolCallId", toolCallId,
                        "toolCallName", "search_knowledge",
                        "parentMessageId", messageId
                    ));
                    
                    // Tool call arguments
                    String toolArgs = "{\"query\":\"" + question + "\", \"max_results\":5}";
                    send.accept("TOOL_CALL_ARGS", Map.of(
                        "toolCallId", toolCallId,
                        "arguments", toolArgs
                    ));
                    
                    // Tool call result
                    String toolResult = "基于问题 \"" + question + "\" 的搜索结果：[相关文档1, 相关文档2, 相关文档3]";
                    send.accept("TOOL_CALL_RESULT", Map.of(
                        "toolCallId", toolCallId,
                        "content", toolResult
                    ));
                    
                    // End tool call
                    send.accept("TOOL_CALL_END", Map.of("toolCallId", toolCallId));
                }
                
                // Step 5: State Updates (AG-UI standard)
                if ("ALL".equals(filter) || "STATE".equals(filter)) {
                    // State snapshot
                    send.accept("STATE_SNAPSHOT", Map.of(
                        "snapshot", Map.of(
                            "progress", 0.8,
                            "status", "completed",
                            "messageCount", 1,
                            "toolCallCount", 1
                        )
                    ));
                    
                    // Messages snapshot
                    send.accept("MESSAGES_SNAPSHOT", Map.of(
                        "messages", List.of(
                            Map.of(
                                "id", messageId,
                                "role", "assistant", 
                                "content", generateResponse(question),
                                "timestamp", System.currentTimeMillis()
                            )
                        )
                    ));
                }
                
                // Step 6: Finish - Emit RUN_FINISHED (AG-UI standard)
                if ("ALL".equals(filter) || "LIFECYCLE".equals(filter)) {
                    send.accept("STEP_FINISHED", Map.of("stepName", "process_question"));
                    send.accept("RUN_FINISHED", Map.of(
                        "result", Map.of(
                            "ok", true,
                            "messageCount", 1,
                            "toolCallCount", 1
                        )
                    ));
                }
                
                emitter.complete();
                
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
    
    /**
     * Generate a response based on the user's question
     * This simulates an AI agent's response generation
     */
    private String generateResponse(String question) {
        if (question == null || question.trim().isEmpty()) {
            return "您好！我是AG-UI标准的AI助手。请告诉我您需要什么帮助？";
        }
        
        // Generate contextual response based on question content
        String lowerQuestion = question.toLowerCase();
        
        if (lowerQuestion.contains("你好") || lowerQuestion.contains("hello")) {
            return "您好！很高兴为您服务。我是基于AG-UI标准实现的AI助手，可以回答您的问题并提供帮助。";
        } else if (lowerQuestion.contains("帮助") || lowerQuestion.contains("help")) {
            return "我可以帮助您：\n1. 回答问题\n2. 提供信息\n3. 执行工具调用\n4. 进行对话交流\n\n请告诉我您的具体需求。";
        } else if (lowerQuestion.contains("ag-ui") || lowerQuestion.contains("agui")) {
            return "AG-UI是一个标准化的AI代理协议，它定义了AI系统与前端应用之间的通信标准。我们的实现遵循了官方规范，包括：\n\n• RUN_STARTED - 运行开始\n• TEXT_MESSAGE_CHUNK - 文本消息流\n• TOOL_CALL_CHUNK - 工具调用\n• RUN_FINISHED - 运行完成";
        } else if (lowerQuestion.contains("时间") || lowerQuestion.contains("time")) {
            return "当前时间是：" + new Date().toString() + "\n\n我是实时响应的AI助手，可以为您提供准确的时间信息。";
        } else {
            return "收到您的问题：" + question + "\n\n我正在基于AG-UI标准处理您的请求。这是一个模拟的AI响应，展示了标准的AG-UI事件流程。在实际应用中，这里会连接到真实的AI模型。";
        }
    }
}
