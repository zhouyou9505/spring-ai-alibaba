package com.alibaba.cloud.ai.studio.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AG-UI Stream Controller
 * 
 * Provides SSE streaming endpoint for AG-UI protocol events.
 * Supports 17 event types with configurable scenarios and delays.
 * 
 * @see <a href="https://docs.ag-ui.com/llms-full.txt">AG-UI Documentation</a>
 */
@RestController
@RequestMapping("/agui")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class AguiStreamController {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * AG-UI Stream Endpoint
     * Streams AG-UI protocol events via Server-Sent Events
     * 
     * @param scenario The scenario type (all, chat, tool, state, errors)
     * @param delayMs The delay between events in milliseconds
     * @return Flux of Server-Sent Events with AG-UI protocol events
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "all") String scenario,
            @RequestParam(defaultValue = "120") long delayMs) {
        
        return Flux.interval(Duration.ofMillis(delayMs))
                .flatMap(tick -> generateEventsForScenario(scenario, tick, q))
                .map(event -> ServerSentEvent.<String>builder()
                        .data(event)
                        .build());
    }

    /**
     * Generate events based on scenario type
     */
    
    private Flux<String> generateEventsForScenario(String scenario, Long tick, String userQuestion) {
        switch (scenario.toLowerCase()) {
            case "chat":
                return generateChatScenario(tick, userQuestion);
            case "tool":
                return generateToolScenario(tick, userQuestion);
            case "state":
                return generateStateScenario(tick, userQuestion);
            case "errors":
                return generateErrorsScenario(tick, userQuestion);
            case "all":
            default:
                return generateAllScenario(tick, userQuestion);
        }
    }

    /**
     * Generate chat scenario events
     */
    private Flux<String> generateChatScenario(Long tick, String userQuestion) {
        List<String> events = new ArrayList<>();
        String runId = generateRunId();
        String threadId = generateThreadId();
        String stepName = "step-1";
        
        if (tick == 0) {
            // RUN_STARTED
            events.add(createEvent("RUN_STARTED", Map.of(
                "runId", runId,
                "threadId", threadId,
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 1) {
            // STEP_STARTED
            events.add(createEvent("STEP_STARTED", Map.of(
                "stepName", stepName,
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 2) {
            // TEXT_MESSAGE_START
            events.add(createEvent("TEXT_MESSAGE_START", Map.of(
                "messageId", "msg-1",
                "role", "assistant",
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick >= 3 && tick < 8) {
            // TEXT_MESSAGE_CONTENT (multiple chunks)
            String response = userQuestion != null && !userQuestion.trim().isEmpty() 
                ? "你问的是: \"" + userQuestion + "\"。这是一个模拟的聊天回复，展示了 AG-UI 协议的流式文本消息功能。"
                : "Hello world! This is a chat response";
            
            String[] tokens = response.split("(?<=\\G.{10})"); // 每10个字符分割
            int tokenIndex = (int) (tick - 3);
            if (tokenIndex < tokens.length && !tokens[tokenIndex].trim().isEmpty()) {
                events.add(createEvent("TEXT_MESSAGE_CONTENT", Map.of(
                    "messageId", "msg-1",
                    "delta", tokens[tokenIndex],
                    "timestamp", getCurrentTimestamp()
                )));
            }
        } else if (tick == 8) {
            // TEXT_MESSAGE_END (no content field)
            events.add(createEvent("TEXT_MESSAGE_END", Map.of(
                "messageId", "msg-1",
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 9) {
            // STEP_FINISHED
            events.add(createEvent("STEP_FINISHED", Map.of(
                "stepName", stepName,
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 10) {
            // RUN_FINISHED
            events.add(createEvent("RUN_FINISHED", Map.of(
                "runId", runId,
                "threadId", threadId,
                "result", Map.of("status", "completed"),
                "timestamp", getCurrentTimestamp()
            )));
        }
        
        return Flux.fromIterable(events);
    }

    /**
     * Generate tool scenario events
     */
    private Flux<String> generateToolScenario(Long tick, String userQuestion) {
        List<String> events = new ArrayList<>();
        String runId = generateRunId();
        String threadId = generateThreadId();
        String stepName = "step-1";
        
        if (tick == 0) {
            events.add(createEvent("RUN_STARTED", Map.of(
                "runId", runId,
                "threadId", threadId,
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 1) {
            events.add(createEvent("STEP_STARTED", Map.of(
                "stepName", stepName,
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 2) {
            events.add(createEvent("TOOL_CALL_START", Map.of(
                "toolCallId", "tool-1",
                "toolCallName", "search_api",
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick >= 3 && tick < 6) {
            // TOOL_CALL_ARGS (multiple chunks)
            String query = userQuestion != null && !userQuestion.trim().isEmpty() 
                ? userQuestion 
                : "test";
            String[] args = {"{\"query\":", " \"" + query + "\"", "}"};
            int argIndex = (int) (tick - 3);
            if (argIndex < args.length && !args[argIndex].trim().isEmpty()) {
                events.add(createEvent("TOOL_CALL_ARGS", Map.of(
                    "toolCallId", "tool-1",
                    "delta", args[argIndex],
                    "timestamp", getCurrentTimestamp()
                )));
            }
        } else if (tick == 6) {
            events.add(createEvent("TOOL_CALL_END", Map.of(
                "toolCallId", "tool-1",
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 7) {
            String result = userQuestion != null && !userQuestion.trim().isEmpty() 
                ? "搜索 \"" + userQuestion + "\" 的结果: [相关结果1, 相关结果2, 相关结果3]"
                : "Search results: [result1, result2]";
            
            events.add(createEvent("TOOL_CALL_RESULT", Map.of(
                "messageId", "msg-2",
                "toolCallId", "tool-1",
                "role", "tool",
                "content", result,
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 8) {
            events.add(createEvent("STEP_FINISHED", Map.of(
                "stepName", stepName,
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 9) {
            events.add(createEvent("RUN_FINISHED", Map.of(
                "runId", runId,
                "threadId", threadId,
                "result", Map.of("status", "completed"),
                "timestamp", getCurrentTimestamp()
            )));
        }
        
        return Flux.fromIterable(events);
    }

    /**
     * Generate state scenario events
     */
    private Flux<String> generateStateScenario(Long tick, String userQuestion) {
        List<String> events = new ArrayList<>();
        
        if (tick == 0) {
            events.add(createEvent("RUN_STARTED", Map.of(
                "runId", generateRunId(),
                "threadId", generateThreadId(),
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 1) {
            events.add(createEvent("STATE_SNAPSHOT", Map.of(
                "snapshot", Map.of("progress", 0.0, "status", "initialized"),
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick >= 2 && tick < 12) {
            // STATE_DELTA (progressive updates)
            double progress = (tick - 1) * 0.1;
            if (progress <= 1.0) {
                events.add(createEvent("STATE_DELTA", Map.of(
                    "delta", List.of(Map.of(
                        "op", "replace",
                        "path", "/progress",
                        "value", progress
                    )),
                    "timestamp", getCurrentTimestamp()
                )));
            }
        } else if (tick == 12) {
            String userContent = userQuestion != null && !userQuestion.trim().isEmpty() 
                ? userQuestion 
                : "Hello";
            String assistantContent = userQuestion != null && !userQuestion.trim().isEmpty() 
                ? "我理解你的问题: \"" + userQuestion + "\"。让我为你提供帮助。"
                : "Hi there!";
            
            events.add(createEvent("MESSAGES_SNAPSHOT", Map.of(
                "messages", List.of(
                    Map.of("id", "msg-1", "role", "user", "content", userContent),
                    Map.of("id", "msg-2", "role", "assistant", "content", assistantContent)
                ),
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 13) {
            events.add(createEvent("RUN_FINISHED", Map.of(
                "runId", "",
                "threadId", "",
                "result", Map.of("status", "completed"),
                "timestamp", getCurrentTimestamp()
            )));
        }
        
        return Flux.fromIterable(events);
    }

    /**
     * Generate errors scenario events
     */
    private Flux<String> generateErrorsScenario(Long tick, String userQuestion) {
        List<String> events = new ArrayList<>();
        String runId = generateRunId();
        String threadId = generateThreadId();
        String stepName = "step-1";
        
        if (tick == 0) {
            events.add(createEvent("RUN_STARTED", Map.of(
                "runId", runId,
                "threadId", threadId,
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 1) {
            events.add(createEvent("STEP_STARTED", Map.of(
                "stepName", stepName,
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 2) {
            String errorMsg = userQuestion != null && !userQuestion.trim().isEmpty() 
                ? "处理你的问题 \"" + userQuestion + "\" 时发生错误"
                : "An error occurred during processing";
            
            events.add(createEvent("RUN_ERROR", Map.of(
                "runId", runId,
                "threadId", threadId,
                "message", errorMsg,
                "stack", "Error: Something went wrong\n    at processStep()\n    at main()",
                "timestamp", getCurrentTimestamp()
            )));
        }
        
        return Flux.fromIterable(events);
    }

    /**
     * Generate all scenario events (covers all 17 event types)
     */
    private Flux<String> generateAllScenario(Long tick, String userQuestion) {
        List<String> events = new ArrayList<>();
        String runId = generateRunId();
        String threadId = generateThreadId();
        String stepName = "step-1";
        
        if (tick == 0) {
            events.add(createEvent("RUN_STARTED", Map.of(
                "runId", runId,
                "threadId", threadId,
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 1) {
            events.add(createEvent("STEP_STARTED", Map.of(
                "stepName", stepName,
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 2) {
            // Use CUSTOM for thinking events (not in official 17 types)
            events.add(createEvent("CUSTOM", Map.of(
                "name", "THINKING_START",
                "value", Map.of("status", "processing"),
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 3) {
            events.add(createEvent("CUSTOM", Map.of(
                "name", "THINKING_TEXT_MESSAGE_START",
                "value", Map.of("type", "internal"),
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 4) {
            events.add(createEvent("CUSTOM", Map.of(
                "name", "THINKING_TEXT_MESSAGE_CONTENT",
                "value", "Processing request...",
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 5) {
            events.add(createEvent("CUSTOM", Map.of(
                "name", "THINKING_TEXT_MESSAGE_END",
                "value", Map.of("status", "completed"),
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 6) {
            events.add(createEvent("CUSTOM", Map.of(
                "name", "THINKING_END",
                "value", Map.of("result", "success"),
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 7) {
            events.add(createEvent("TEXT_MESSAGE_START", Map.of(
                "messageId", "msg-1",
                "role", "assistant",
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick >= 8 && tick < 11) {
            String response = userQuestion != null && !userQuestion.trim().isEmpty() 
                ? "你问的是: \"" + userQuestion + "\"。这是一个完整的 AG-UI 事件演示。"
                : "Hello world";
            
            String[] tokens = response.split("(?<=\\G.{8})"); // 每8个字符分割
            int tokenIndex = (int) (tick - 8);
            if (tokenIndex < tokens.length && !tokens[tokenIndex].trim().isEmpty()) {
                events.add(createEvent("TEXT_MESSAGE_CONTENT", Map.of(
                    "messageId", "msg-1",
                    "delta", tokens[tokenIndex],
                    "timestamp", getCurrentTimestamp()
                )));
            }
        } else if (tick == 11) {
            // TEXT_MESSAGE_END should not have content field
            events.add(createEvent("TEXT_MESSAGE_END", Map.of(
                "messageId", "msg-1",
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 12) {
            events.add(createEvent("TOOL_CALL_START", Map.of(
                "toolCallId", "tool-1",
                "toolCallName", "demo_tool",
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 13) {
            String args = userQuestion != null && !userQuestion.trim().isEmpty() 
                ? "{\"query\": \"" + userQuestion + "\", \"param\": \"value\"}"
                : "{\"param\": \"value\"}";
            
            events.add(createEvent("TOOL_CALL_ARGS", Map.of(
                "toolCallId", "tool-1",
                "delta", args,
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 14) {
            events.add(createEvent("TOOL_CALL_END", Map.of(
                "toolCallId", "tool-1",
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 15) {
            String result = userQuestion != null && !userQuestion.trim().isEmpty() 
                ? "基于你的问题 \"" + userQuestion + "\" 的工具执行结果"
                : "Tool execution result";
            
            events.add(createEvent("TOOL_CALL_RESULT", Map.of(
                "messageId", "msg-2",
                "toolCallId", "tool-1",
                "role", "tool",
                "content", result,
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 16) {
            events.add(createEvent("STATE_SNAPSHOT", Map.of(
                "snapshot", Map.of("progress", 0.5, "status", "processing"),
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 17) {
            events.add(createEvent("STATE_DELTA", Map.of(
                "delta", List.of(Map.of(
                    "op", "replace",
                    "path", "/progress",
                    "value", 0.8
                )),
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 18) {
            String userContent = userQuestion != null && !userQuestion.trim().isEmpty() 
                ? userQuestion 
                : "Hello";
            String assistantContent = userQuestion != null && !userQuestion.trim().isEmpty() 
                ? "我理解你的问题: \"" + userQuestion + "\"。这是一个完整的 AG-UI 事件演示。"
                : "Hello world";
            
            events.add(createEvent("MESSAGES_SNAPSHOT", Map.of(
                "messages", List.of(
                    Map.of("id", "msg-1", "role", "user", "content", userContent),
                    Map.of("id", "msg-2", "role", "assistant", "content", assistantContent)
                ),
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 19) {
            events.add(createEvent("RAW", Map.of(
                "event", Map.of("type", "custom", "data", "Raw event data"),
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 20) {
            String customValue = userQuestion != null && !userQuestion.trim().isEmpty() 
                ? "基于你的问题 \"" + userQuestion + "\" 的自定义事件值"
                : "Custom event value";
            
            events.add(createEvent("CUSTOM", Map.of(
                "name", "demo_event",
                "value", customValue,
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 21) {
            events.add(createEvent("STEP_FINISHED", Map.of(
                "stepName", stepName,
                "timestamp", getCurrentTimestamp()
            )));
        } else if (tick == 22) {
            events.add(createEvent("RUN_FINISHED", Map.of(
                "runId", runId,
                "threadId", threadId,
                "result", Map.of("status", "completed"),
                "timestamp", getCurrentTimestamp()
            )));
        }
        
        return Flux.fromIterable(events);
    }

    /**
     * Create an AG-UI event with the given type and data
     */
    private String createEvent(String type, Map<String, Object> data) {
        try {
            Map<String, Object> event = new HashMap<>(data);
            event.put("type", type);
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            return "{\"type\":\"" + type + "\",\"error\":\"Serialization failed\"}";
        }
    }

    /**
     * Generate a unique run ID
     */
    private String generateRunId() {
        return "run-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Generate a unique thread ID
     */
    private String generateThreadId() {
        return "thread-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Get current timestamp in seconds
     */
    private int getCurrentTimestamp() {
        return (int) (System.currentTimeMillis() / 1000);
    }
}
