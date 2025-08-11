package com.alibaba.cloud.ai.studio.admin.controller;

import com.agui.event.*;
import com.agui.message.UserMessage;
import com.agui.types.RunAgentInput;
import com.alibaba.cloud.ai.studio.admin.service.AguiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AG-UI Protocol Controller
 * 
 * Implements the AG-UI protocol for AI agent communication.
 * Supports streaming responses and AG-UI standard events.
 * 
 * @see <a href="https://docs.ag-ui.com/llms-full.txt">AG-UI Documentation</a>
 */
@RestController
@RequestMapping("/api/agui")
@CrossOrigin(origins = "*")
public class AguiController {

    @Autowired
    private AguiService aguiService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * AG-UI Run Agent Endpoint
     * Handles agent execution requests following AG-UI protocol
     * 
     * @param request The run agent request
     * @return Server-Sent Events stream with AG-UI protocol events
     */
    @PostMapping(value = "/run", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter runAgent(@RequestBody RunAgentInput request) {
        SseEmitter emitter = new SseEmitter(0L); // No timeout
        
        try {
            // Emit RUN_STARTED event
            RunStartedEvent runStartedEvent = new RunStartedEvent();
            runStartedEvent.setRunId(request.runId());
            runStartedEvent.setThreadId(request.threadId());
            runStartedEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
            
            emitter.send(SseEmitter.event()
                .name("event")
                .data(runStartedEvent));

            // Process the agent request and stream responses
            aguiService.processAgentRequest(request, emitter);

        } catch (Exception e) {
            // Emit RUN_ERROR event
            try {
                RunErrorEvent runErrorEvent = new RunErrorEvent();
                runErrorEvent.setRawEvent(Map.of("runId", request.runId(), "error", e.getMessage()));
                runErrorEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
                
                emitter.send(SseEmitter.event()
                    .name("event")
                    .data(runErrorEvent));
            } catch (IOException ex) {
                // Log error
            }
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * AG-UI Chat Endpoint (Simplified)
     * Handles simple chat requests following AG-UI protocol
     * 
     * @param request The chat request
     * @return Server-Sent Events stream with AG-UI protocol events
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody AguiChatRequest request) {
        // Convert to RunAgentInput
        UserMessage userMessage = new UserMessage();
        userMessage.setId(request.getRunId());
        userMessage.setContent(request.getContent());
        
        RunAgentInput runAgentInput = new RunAgentInput(
            request.getThreadId() != null ? request.getThreadId() : UUID.randomUUID().toString(),
            request.getRunId(),
            null, // state
            List.of(userMessage),
            null, // tools
            null, // context
            null  // forwardedProps
        );
        
        return runAgent(runAgentInput);
    }

    /**
     * AG-UI Demo Endpoint - Demonstrates all event types
     * 
     * @return Server-Sent Events stream with all AG-UI event types
     */
    @PostMapping(value = "/demo", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter demo(@RequestBody AguiDemoRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        
        try {
            aguiService.processDemoRequest(request, emitter);
        } catch (Exception e) {
            try {
                RunErrorEvent runErrorEvent = new RunErrorEvent();
                runErrorEvent.setRawEvent(Map.of("runId", request.getRunId(), "error", e.getMessage()));
                runErrorEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
                
                emitter.send(SseEmitter.event()
                    .name("event")
                    .data(runErrorEvent));
            } catch (IOException ex) {
                // Log error
            }
            emitter.completeWithError(e);
        }
        
        return emitter;
    }

    /**
     * AG-UI Health Check Endpoint
     * 
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "protocol", "AG-UI",
            "version", "1.0.0",
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * AG-UI Protocol Info Endpoint
     * 
     * @return Protocol information
     */
    @GetMapping("/protocol")
    public ResponseEntity<Map<String, Object>> protocolInfo() {
        return ResponseEntity.ok(Map.of(
            "name", "AG-UI Protocol",
            "version", "1.0.0",
            "features", new String[]{
                "agentic_chat",
                "streaming_responses",
                "tool_calls",
                "run_events",
                "step_events",
                "state_management",
                "thinking_events",
                "custom_events"
            },
            "supported_events", new String[]{
                // Text message events
                "TEXT_MESSAGE_START", "TEXT_MESSAGE_CONTENT", "TEXT_MESSAGE_END", "TEXT_MESSAGE_CHUNK",
                // Thinking events
                "THINKING_START", "THINKING_END",
                "THINKING_TEXT_MESSAGE_START", "THINKING_TEXT_MESSAGE_CONTENT", "THINKING_TEXT_MESSAGE_END",
                // Tool call events
                "TOOL_CALL_START", "TOOL_CALL_ARGS", "TOOL_CALL_END", "TOOL_CALL_CHUNK", "TOOL_CALL_RESULT",
                // State management events
                "STATE_SNAPSHOT", "STATE_DELTA", "MESSAGES_SNAPSHOT",
                // Lifecycle events
                "RUN_STARTED", "RUN_FINISHED", "RUN_ERROR",
                "STEP_STARTED", "STEP_FINISHED",
                // Special events
                "RAW", "CUSTOM"
            }
        ));
    }

    /**
     * AG-UI Chat Request DTO (Simplified)
     */
    public static class AguiChatRequest {
        private String runId;
        private String threadId;
        private String content;
        private String model;
        private Map<String, Object> config;

        // Getters and Setters
        public String getRunId() { return runId; }
        public void setRunId(String runId) { this.runId = runId; }

        public String getThreadId() { return threadId; }
        public void setThreadId(String threadId) { this.threadId = threadId; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public Map<String, Object> getConfig() { return config; }
        public void setConfig(Map<String, Object> config) { this.config = config; }
    }

    /**
     * AG-UI Demo Request DTO
     */
    public static class AguiDemoRequest {
        private String runId;
        private String threadId;
        private String demoType; // "full", "lifecycle", "text", "tool", "state", "thinking", "custom"
        private Map<String, Object> config;

        // Getters and Setters
        public String getRunId() { return runId; }
        public void setRunId(String runId) { this.runId = runId; }

        public String getThreadId() { return threadId; }
        public void setThreadId(String threadId) { this.threadId = threadId; }

        public String getDemoType() { return demoType; }
        public void setDemoType(String demoType) { this.demoType = demoType; }

        public Map<String, Object> getConfig() { return config; }
        public void setConfig(Map<String, Object> config) { this.config = config; }
    }
}
