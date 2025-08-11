package com.alibaba.cloud.ai.studio.admin.service.impl;

import com.agui.event.*;
import com.agui.message.BaseMessage;
import com.agui.types.RunAgentInput;
import com.alibaba.cloud.ai.studio.admin.controller.AguiController.AguiChatRequest;
import com.alibaba.cloud.ai.studio.admin.controller.AguiController.AguiDemoRequest;
import com.alibaba.cloud.ai.studio.admin.service.AguiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * AG-UI Protocol Service Implementation
 * 
 * Provides concrete implementation for AG-UI protocol handling.
 * Implements streaming responses and event management.
 */
@Service
public class AguiServiceImpl implements AguiService {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void processAgentRequest(RunAgentInput request, SseEmitter emitter) {
        // Validate the request
        if (request.runId() == null || request.runId().trim().isEmpty()) {
            sendErrorEvent(emitter, request.runId(), "Invalid request parameters");
            return;
        }

        // Process the request asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                // Emit STEP_STARTED event
                StepStartedEvent stepStartedEvent = new StepStartedEvent();
                stepStartedEvent.setRawEvent(Map.of(
                    "runId", request.runId(),
                    "stepId", "agent-processing",
                    "stepType", "agent"
                ));
                stepStartedEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
                sendEvent(emitter, stepStartedEvent);

                // Process the agent request with streaming response
                processAgentStreamingResponse(request, emitter);
                
                // Emit STEP_FINISHED event
                StepFinishedEvent stepFinishedEvent = new StepFinishedEvent();
                stepFinishedEvent.setRawEvent(Map.of(
                    "runId", request.runId(),
                    "stepId", "agent-processing",
                    "stepType", "agent"
                ));
                stepFinishedEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
                sendEvent(emitter, stepFinishedEvent);

                // Send RUN_FINISHED event
                RunFinishedEvent runFinishedEvent = new RunFinishedEvent();
                runFinishedEvent.setRawEvent(Map.of("runId", request.runId()));
                runFinishedEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
                sendEvent(emitter, runFinishedEvent);
                
                emitter.complete();
                
            } catch (Exception e) {
                sendErrorEvent(emitter, request.runId(), e.getMessage());
                emitter.completeWithError(e);
            }
        });
    }

    @Override
    public void processChatRequest(AguiChatRequest request, SseEmitter emitter) {
        // Validate the request
        if (!validateRequest(request)) {
            sendErrorEvent(emitter, request.getRunId(), "Invalid request parameters");
            return;
        }

        // Process the request asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                // Emit STEP_STARTED event
                StepStartedEvent stepStartedEvent = new StepStartedEvent();
                stepStartedEvent.setRawEvent(Map.of(
                    "runId", request.getRunId(),
                    "stepId", "chat-processing",
                    "stepType", "chat"
                ));
                stepStartedEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
                sendEvent(emitter, stepStartedEvent);

                // Simulate AI processing with streaming response
                processStreamingResponse(request, emitter);
                
                // Emit STEP_FINISHED event
                StepFinishedEvent stepFinishedEvent = new StepFinishedEvent();
                stepFinishedEvent.setRawEvent(Map.of(
                    "runId", request.getRunId(),
                    "stepId", "chat-processing",
                    "stepType", "chat"
                ));
                stepFinishedEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
                sendEvent(emitter, stepFinishedEvent);

                // Send RUN_FINISHED event
                RunFinishedEvent runFinishedEvent = new RunFinishedEvent();
                runFinishedEvent.setRawEvent(Map.of("runId", request.getRunId()));
                runFinishedEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
                sendEvent(emitter, runFinishedEvent);
                
                emitter.complete();
                
            } catch (Exception e) {
                sendErrorEvent(emitter, request.getRunId(), e.getMessage());
                emitter.completeWithError(e);
            }
        });
    }

    @Override
    public void processDemoRequest(AguiDemoRequest request, SseEmitter emitter) {
        CompletableFuture.runAsync(() -> {
            try {
                String demoType = request.getDemoType() != null ? request.getDemoType() : "full";
                
                switch (demoType) {
                    case "lifecycle":
                        demonstrateLifecycleEvents(emitter, request.getRunId());
                        break;
                    case "text":
                        demonstrateTextMessageEvents(emitter, request.getRunId());
                        break;
                    case "tool":
                        demonstrateToolCallEvents(emitter, request.getRunId());
                        break;
                    case "state":
                        demonstrateStateManagementEvents(emitter, request.getRunId());
                        break;
                    case "thinking":
                        demonstrateThinkingEvents(emitter, request.getRunId());
                        break;
                    case "custom":
                        demonstrateCustomEvents(emitter, request.getRunId());
                        break;
                    default:
                        demonstrateAllEventTypes(emitter, request.getRunId());
                        break;
                }
                
                emitter.complete();
                
            } catch (Exception e) {
                sendErrorEvent(emitter, request.getRunId(), e.getMessage());
                emitter.completeWithError(e);
            }
        });
    }

    @Override
    public String generateRunId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public boolean validateRequest(AguiChatRequest request) {
        return request != null 
            && request.getRunId() != null 
            && !request.getRunId().trim().isEmpty()
            && request.getContent() != null 
            && !request.getContent().trim().isEmpty();
    }

    /**
     * Process agent streaming response for AG-UI
     */
    private void processAgentStreamingResponse(RunAgentInput request, SseEmitter emitter) {
        try {
            // Emit TEXT_MESSAGE_START event
            TextMessageStartEvent textMessageStartEvent = new TextMessageStartEvent();
            textMessageStartEvent.setMessageId(UUID.randomUUID().toString());
            textMessageStartEvent.setRawEvent(Map.of("runId", request.runId()));
            textMessageStartEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
            sendEvent(emitter, textMessageStartEvent);

            // Generate AI response based on messages
            List<BaseMessage> messages = request.messages();
            if (messages != null && !messages.isEmpty()) {
                BaseMessage lastMessage = messages.get(messages.size() - 1);
                String response = generateSampleResponse(lastMessage.getContent());
                
                // Stream the response in chunks
                streamTextChunks(emitter, request.runId(), response);
            }
            
            // Emit TEXT_MESSAGE_END event
            TextMessageEndEvent textMessageEndEvent = new TextMessageEndEvent();
            textMessageEndEvent.setMessageId(UUID.randomUUID().toString());
            textMessageEndEvent.setRawEvent(Map.of("runId", request.runId()));
            textMessageEndEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
            sendEvent(emitter, textMessageEndEvent);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to process agent streaming response", e);
        }
    }

    /**
     * Process streaming response for AG-UI chat
     */
    private void processStreamingResponse(AguiChatRequest request, SseEmitter emitter) {
        try {
            // Emit TEXT_MESSAGE_START event
            TextMessageStartEvent textMessageStartEvent = new TextMessageStartEvent();
            textMessageStartEvent.setMessageId(UUID.randomUUID().toString());
            textMessageStartEvent.setRawEvent(Map.of("runId", request.getRunId()));
            textMessageStartEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
            sendEvent(emitter, textMessageStartEvent);

            // Simulate AI response generation
            String response = generateSampleResponse(request.getContent());
            
            // Stream the response in chunks
            streamTextChunks(emitter, request.getRunId(), response);
            
            // Emit TEXT_MESSAGE_END event
            TextMessageEndEvent textMessageEndEvent = new TextMessageEndEvent();
            textMessageEndEvent.setMessageId(UUID.randomUUID().toString());
            textMessageEndEvent.setRawEvent(Map.of("runId", request.getRunId()));
            textMessageEndEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
            sendEvent(emitter, textMessageEndEvent);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to process streaming response", e);
        }
    }

    /**
     * Demonstrate all AG-UI event types
     */
    private void demonstrateAllEventTypes(SseEmitter emitter, String runId) throws IOException {
        // Lifecycle events
        demonstrateLifecycleEvents(emitter, runId);
        
        // Text message events
        demonstrateTextMessageEvents(emitter, runId);
        
        // Tool call events
        demonstrateToolCallEvents(emitter, runId);
        
        // State management events
        demonstrateStateManagementEvents(emitter, runId);
        
        // Thinking events
        demonstrateThinkingEvents(emitter, runId);
        
        // Custom events
        demonstrateCustomEvents(emitter, runId);
    }

    /**
     * Demonstrate lifecycle events
     */
    private void demonstrateLifecycleEvents(SseEmitter emitter, String runId) throws IOException {
        // STEP_STARTED
        StepStartedEvent stepStartedEvent = new StepStartedEvent();
        stepStartedEvent.setRawEvent(Map.of(
            "runId", runId,
            "stepId", "demo-lifecycle",
            "stepType", "demonstration"
        ));
        stepStartedEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
        sendEvent(emitter, stepStartedEvent);
        
        // Simulate some processing time
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // STEP_FINISHED
        StepFinishedEvent stepFinishedEvent = new StepFinishedEvent();
        stepFinishedEvent.setRawEvent(Map.of(
            "runId", runId,
            "stepId", "demo-lifecycle",
            "stepType", "demonstration"
        ));
        stepFinishedEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
        sendEvent(emitter, stepFinishedEvent);
    }

    /**
     * Demonstrate text message events
     */
    private void demonstrateTextMessageEvents(SseEmitter emitter, String runId) throws IOException {
        String messageId = UUID.randomUUID().toString();
        
        // TEXT_MESSAGE_START
        TextMessageStartEvent textMessageStartEvent = new TextMessageStartEvent();
        textMessageStartEvent.setMessageId(messageId);
        textMessageStartEvent.setRawEvent(Map.of("runId", runId));
        textMessageStartEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
        sendEvent(emitter, textMessageStartEvent);
        
        // TEXT_MESSAGE_CONTENT
        String[] words = {"This", "is", "a", "demonstration", "of", "text", "message", "events"};
        for (int i = 0; i < words.length; i++) {
            TextMessageContentEvent textMessageContentEvent = new TextMessageContentEvent();
            textMessageContentEvent.setMessageId(messageId);
            textMessageContentEvent.setDelta(words[i] + (i < words.length - 1 ? " " : ""));
            textMessageContentEvent.setRawEvent(Map.of("runId", runId));
            textMessageContentEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
            sendEvent(emitter, textMessageContentEvent);
            
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // TEXT_MESSAGE_END
        TextMessageEndEvent textMessageEndEvent = new TextMessageEndEvent();
        textMessageEndEvent.setMessageId(messageId);
        textMessageEndEvent.setRawEvent(Map.of("runId", runId));
        textMessageEndEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
        sendEvent(emitter, textMessageEndEvent);
    }

    /**
     * Demonstrate tool call events
     */
    private void demonstrateToolCallEvents(SseEmitter emitter, String runId) throws IOException {
        String toolCallId = UUID.randomUUID().toString();
        
        // TOOL_CALL_START
        ToolCallStartEvent toolCallStartEvent = new ToolCallStartEvent();
        toolCallStartEvent.setRawEvent(Map.of(
            "runId", runId,
            "toolCallId", toolCallId,
            "toolName", "demo_tool"
        ));
        toolCallStartEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
        sendEvent(emitter, toolCallStartEvent);
        
        // TOOL_CALL_ARGS
        ToolCallArgsEvent toolCallArgsEvent = new ToolCallArgsEvent();
        toolCallArgsEvent.setRawEvent(Map.of(
            "runId", runId,
            "toolCallId", toolCallId,
            "args", Map.of(
                "param1", "value1",
                "param2", "value2",
                "param3", Map.of("nested", "value")
            )
        ));
        toolCallArgsEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
        sendEvent(emitter, toolCallArgsEvent);
        
        // Simulate tool execution
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // TOOL_CALL_END
        ToolCallEndEvent toolCallEndEvent = new ToolCallEndEvent();
        toolCallEndEvent.setRawEvent(Map.of(
            "runId", runId,
            "toolCallId", toolCallId,
            "result", "Tool execution completed successfully"
        ));
        toolCallEndEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
        sendEvent(emitter, toolCallEndEvent);
    }

    /**
     * Demonstrate thinking events
     */
    private void demonstrateThinkingEvents(SseEmitter emitter, String runId) throws IOException {
        // THINKING_START
        ThinkingStartEvent thinkingStartEvent = new ThinkingStartEvent();
        thinkingStartEvent.setRawEvent(Map.of("runId", runId));
        thinkingStartEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
        sendEvent(emitter, thinkingStartEvent);
        
        // Simulate thinking process
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // THINKING_END
        ThinkingEndEvent thinkingEndEvent = new ThinkingEndEvent();
        thinkingEndEvent.setRawEvent(Map.of("runId", runId));
        thinkingEndEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
        sendEvent(emitter, thinkingEndEvent);
    }

    /**
     * Demonstrate state management events
     */
    private void demonstrateStateManagementEvents(SseEmitter emitter, String runId) throws IOException {
        // STATE_SNAPSHOT
        StateSnapshotEvent stateSnapshotEvent = new StateSnapshotEvent();
        stateSnapshotEvent.setRawEvent(Map.of(
            "runId", runId,
            "state", Map.of(
                "conversationId", "conv-123",
                "userId", "user-456",
                "sessionData", Map.of(
                    "startTime", System.currentTimeMillis(),
                    "messageCount", 5,
                    "context", "demo session"
                )
            )
        ));
        stateSnapshotEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
        sendEvent(emitter, stateSnapshotEvent);
        
        // STATE_DELTA
        StateDeltaEvent stateDeltaEvent = new StateDeltaEvent();
        stateDeltaEvent.setRawEvent(Map.of(
            "runId", runId,
            "delta", Map.of(
                "operation", "update",
                "path", "sessionData.messageCount",
                "value", 6,
                "previousValue", 5
            )
        ));
        stateDeltaEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
        sendEvent(emitter, stateDeltaEvent);
        
        // MESSAGES_SNAPSHOT
        MessagesSnapshotEvent messagesSnapshotEvent = new MessagesSnapshotEvent();
        messagesSnapshotEvent.setRawEvent(Map.of(
            "runId", runId,
            "messages", new String[]{
                "Hello, how can I help you?",
                "I need assistance with AG-UI events",
                "I'll demonstrate all event types for you"
            }
        ));
        messagesSnapshotEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
        sendEvent(emitter, messagesSnapshotEvent);
    }

    /**
     * Demonstrate custom events
     */
    private void demonstrateCustomEvents(SseEmitter emitter, String runId) throws IOException {
        // RAW event
        RawEvent rawEvent = new RawEvent();
        rawEvent.setRawEvent(Map.of(
            "runId", runId,
            "rawData", "This is raw event data in any format",
            "format", "text"
        ));
        rawEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
        sendEvent(emitter, rawEvent);
        
        // CUSTOM event
        CustomEvent customEvent = new CustomEvent();
        customEvent.setRawEvent(Map.of(
            "runId", runId,
            "customType", "demo_custom_event",
            "customData", Map.of(
                "feature", "custom_events",
                "description", "This demonstrates custom event types",
                "metadata", Map.of(
                    "version", "1.0.0",
                    "author", "AG-UI Demo"
                )
            )
        ));
        customEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
        sendEvent(emitter, customEvent);
    }

    /**
     * Generate sample AI response
     */
    private String generateSampleResponse(String userMessage) {
        // This is a sample response generator
        // In a real implementation, this would call an AI model
        if (userMessage.toLowerCase().contains("hello")) {
            return "Hello! I'm an AI assistant powered by AG-UI protocol. How can I help you today?";
        } else if (userMessage.toLowerCase().contains("help")) {
            return "I can help you with various tasks. I support the AG-UI protocol for AI agent communication.";
        } else {
            return "I understand you said: \"" + userMessage + "\". This is a sample response from the AG-UI controller.";
        }
    }

    /**
     * Stream text chunks as AG-UI events
     */
    private void streamTextChunks(SseEmitter emitter, String runId, String text) throws IOException {
        // Split text into chunks for streaming
        String[] chunks = text.split(" ");
        
        for (int i = 0; i < chunks.length; i++) {
            String chunk = chunks[i] + (i < chunks.length - 1 ? " " : "");
            
            TextMessageContentEvent textMessageContentEvent = new TextMessageContentEvent();
            textMessageContentEvent.setMessageId(UUID.randomUUID().toString());
            textMessageContentEvent.setDelta(chunk);
            textMessageContentEvent.setRawEvent(Map.of("runId", runId));
            textMessageContentEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
            sendEvent(emitter, textMessageContentEvent);
            
            // Add small delay to simulate streaming
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Send AG-UI event
     */
    private void sendEvent(SseEmitter emitter, BaseEvent event) {
        try {
            emitter.send(SseEmitter.event()
                .name("event")
                .data(event));
                
        } catch (IOException e) {
            // Log error
        }
    }

    /**
     * Send error event
     */
    private void sendErrorEvent(SseEmitter emitter, String runId, String errorMessage) {
        RunErrorEvent runErrorEvent = new RunErrorEvent();
        runErrorEvent.setRawEvent(Map.of("runId", runId, "error", errorMessage));
        runErrorEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
        sendEvent(emitter, runErrorEvent);
    }
}
