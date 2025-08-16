package com.alibaba.cloud.ai.studio.admin.controller;

import com.agui.client.AbstractAgent;
import com.agui.event.*;
import com.agui.message.BaseMessage;
import com.agui.message.AssistantMessage;
import com.agui.types.RunAgentInput;
import com.agui.types.Tool;
import com.agui.types.State;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * AG-UI Stream Controller - Standard Implementation
 * Extends AbstractAgent to provide SSE streaming endpoint for AG-UI protocol events
 * following the official specification.
 *
 * @see <a href="https://docs.ag-ui.com/concepts/events">AG-UI Events Documentation</a>
 * @see <a href="https://docs.ag-ui.com/concepts/agents">AG-UI Agents Documentation</a>
 * @see <a href="https://docs.ag-ui.com/concepts/messages">AG-UI Messages Documentation</a>
 * @see <a href="https://docs.ag-ui.com/concepts/tools">AG-UI Tools Documentation</a>
 */
@RestController
@RequestMapping("/api/agui")
@CrossOrigin(origins = {"localhost", "http://localhost:8000"})
public class AguiStreamController extends AbstractAgent {

    /**
     * Constructor for AG-UI Stream Controller
     */
    public AguiStreamController() {
        super(
                "agui-stream-controller",
                "AG-UI Standard Stream Controller for AI Studio",
                null, // threadId will be generated
                new ArrayList<>(),
                new State(),
                false // debug mode
        );
    }

    /**
     * AG-UI Init Endpoint - Initialize chat history
     * Returns MessagesSnapshot for frontend to initialize chat context
     *
     * @param threadId The thread ID to get chat history for
     * @return MessagesSnapshotEvent containing all messages for the thread
     */
    @GetMapping(value = "/init/{threadId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public MessagesSnapshotEvent init(@PathVariable String threadId) {
        // Set the thread ID
        this.threadId = threadId;
        
        // Create and return MessagesSnapshot with current messages
        MessagesSnapshotEvent messagesSnapshot = new MessagesSnapshotEvent();
        messagesSnapshot.setMessages(this.messages);
        messagesSnapshot.setTimestamp((int) (System.currentTimeMillis() / 1000));
        
        return messagesSnapshot;
    }

    /**
     * AG-UI Stream Endpoint - Standard Implementation
     * Streams AG-UI protocol events via Server-Sent Events following the official specification
     * Note: This endpoint no longer returns MessagesSnapshot as it's handled by /init endpoint
     *
     * @param input The RunAgentInput containing threadId, runId, messages, tools, etc.
     * @return SseEmitter for streaming AG-UI protocol events
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody RunAgentInput input) {
        return createStream(input);
    }

    /**
     * Create the AG-UI stream using AbstractAgent's event handling
     * Follows the official AG-UI event flow patterns:
     * 1. Lifecycle Pattern: RunStarted → StepStarted → StepFinished → RunFinished
     * 2. Start-Content-End Pattern: TextMessageStart → TextMessageContent → TextMessageEnd
     * 3. Tool Call Pattern: ToolCallStart → ToolCallArgs → ToolCallEnd
     * 4. Snapshot-Delta Pattern: StateSnapshot
     * 
     * Note: MessagesSnapshot is now handled by separate /init endpoint
     * Frontend should call /init/{threadId} before starting stream to get chat history
     */
    private SseEmitter createStream(RunAgentInput input) {
        SseEmitter emitter = new SseEmitter(0L);

        // Create a simple event handler that sends events via SSE
        Consumer<BaseEvent> sseEventHandler = (event) -> {
            try {
                // Set timestamp for the event (AG-UI standard)
                event.setTimestamp((int) (System.currentTimeMillis() / 1000));

                // Send event via SSE
                emitter.send(SseEmitter.event().data(event));

                // Small delay for better UX
                Thread.sleep(200);
            } catch (Exception e) {
                System.err.println("Error sending event via SSE: " + e.getMessage());
            }
        };

        // Set thread ID if provided
        if (input.threadId() != null) {
            this.threadId = input.threadId();
        }

        // Add messages from input
        if (input.messages() != null) {
            this.addMessages(input.messages());
        }

        // Run the agent directly with the SSE event handler
        this.run(input, sseEventHandler)
                .exceptionally(throwable -> {
                    try {
                        // Send error event if possible
                        RunErrorEvent errorEvent = new RunErrorEvent();
                        errorEvent.setMessage("Error occurred during processing: " + throwable.getMessage());
                        errorEvent.setCode("PROCESSING_ERROR");
                        errorEvent.setTimestamp((int) (System.currentTimeMillis() / 1000));
                        emitter.send(SseEmitter.event().data(errorEvent));
                        emitter.completeWithError(throwable);
                    } catch (Exception e) {
                        emitter.completeWithError(throwable);
                    }
                    return null;
                });

        return emitter;
    }

    /**
     * Implementation of AbstractAgent's abstract run method
     * This method implements the core AG-UI event flow according to the standard:
     * 
     * 1. RunStarted - Initialize the run
     * 2. StepStarted - Begin processing
     * 3. TextMessage streaming - Stream the response
     * 4. Tool calls processing - Handle any tool executions
     * 5. StateSnapshot - Provide current state
     * 6. StepFinished - Complete processing
     * 7. RunFinished - End the run
     * 
     * Note: MessagesSnapshot is handled by separate /init endpoint
     */
    @Override
    protected CompletableFuture<Void> run(RunAgentInput input, Consumer<BaseEvent> eventHandler) {
        return CompletableFuture.runAsync(() -> {
            try {
                String messageId = "msg-" + UUID.randomUUID().toString().substring(0, 8);

                // 1. LIFECYCLE PATTERN: Start the run
                RunStartedEvent runStarted = new RunStartedEvent();
                runStarted.setThreadId(this.threadId);
                runStarted.setRunId(input.runId());
                emitEvent(runStarted, eventHandler);

                // Note: MessagesSnapshot is now handled by /init endpoint
                // Frontend should call /init/{threadId} before starting stream to get chat history

                // Start processing step
                StepStartedEvent stepStarted = new StepStartedEvent();
                stepStarted.setStepName("process_question");
                emitEvent(stepStarted, eventHandler);

                // Get the last user message content for processing
                String userQuestion = "";
                if (input.messages() != null && !input.messages().isEmpty()) {
                    BaseMessage lastMessage = input.messages().get(input.messages().size() - 1);
                    userQuestion = lastMessage.getContent();
                }

                // 2. START-CONTENT-END PATTERN: Text message streaming
                TextMessageStartEvent textStart = new TextMessageStartEvent(messageId, "assistant");
                emitEvent(textStart, eventHandler);

                // Generate response based on user question
                String response = generateResponse(userQuestion);

                // Split response into chunks and emit TEXT_MESSAGE_CONTENT (AG-UI standard)
                String[] chunks = response.split("(?<=\\G.{10})"); // Split every 10 characters for realistic streaming

                for (String chunk : chunks) {
                    if (!chunk.trim().isEmpty()) {
                        TextMessageContentEvent textContent = new TextMessageContentEvent(messageId, chunk);
                        emitEvent(textContent, eventHandler);
                    }
                }

                // End text message
                TextMessageEndEvent textEnd = new TextMessageEndEvent();
                textEnd.setMessageId(messageId);
                emitEvent(textEnd, eventHandler);

                // 3. TOOL CALL PATTERN: Process tools if provided
                List<String> toolCallIds = new ArrayList<>();
                if (input.tools() != null && !input.tools().isEmpty()) {
                    for (Tool tool : input.tools()) {
                        String toolCallId = "tool-" + UUID.randomUUID().toString().substring(0, 8);
                        toolCallIds.add(toolCallId); // Store the toolCallId for later use

                        // Start tool call
                        ToolCallStartEvent toolStart = new ToolCallStartEvent();
                        toolStart.setToolCallId(toolCallId);
                        toolStart.setToolCallName(tool.name());
                        toolStart.setParentMessageId(messageId);
                        emitEvent(toolStart, eventHandler);

                        // Tool call arguments (using tool parameters)
                        if (tool.parameters() != null) {
                            ToolCallArgsEvent toolArgs = new ToolCallArgsEvent();
                            toolArgs.setToolCallId(toolCallId);
                            toolArgs.setDelta(tool.parameters().toString());
                            emitEvent(toolArgs, eventHandler);
                        }

                        // End tool call
                        ToolCallEndEvent toolEnd = new ToolCallEndEvent();
                        toolEnd.setToolCallId(toolCallId);
                        emitEvent(toolEnd, eventHandler);

                        // Tool call result according to AG-UI standard
                        ToolCallResultEvent toolResult = new ToolCallResultEvent();
                        toolResult.setMessageId(messageId);
                        toolResult.setToolCallId(toolCallId);
                        toolResult.setContent("基于工具 " + tool.name() + " 的执行结果：[相关文档1, 相关文档2, 相关文档3]");
                        toolResult.setRole("tool");
                        emitEvent(toolResult, eventHandler);
                    }
                }

                // 4. SNAPSHOT-DELTA PATTERN: State management
                // State snapshot
                StateSnapshotEvent stateSnapshot = new StateSnapshotEvent();
                stateSnapshot.setSnapshot(Map.of(
                        "progress", 0.8,
                        "status", "completed",
                        "messageCount", input.messages() != null ? input.messages().size() + 1 : 1,
                        "toolCallCount", input.tools() != null ? input.tools().size() : 1
                ));
                emitEvent(stateSnapshot, eventHandler);

                // Create and add the new assistant message to the agent's message state
                AssistantMessage assistantMessage = new AssistantMessage();
                assistantMessage.setId(messageId);
                assistantMessage.setContent(response);
                
                // Add tool calls to the assistant message for MESSAGES_SNAPSHOT
                if (input.tools() != null && !input.tools().isEmpty()) {
                    List<com.agui.types.ToolCall> toolCalls = new ArrayList<>();
                    for (int i = 0; i < input.tools().size(); i++) {
                        Tool tool = input.tools().get(i);
                        String toolCallId = toolCallIds.get(i);
                        
                        com.agui.types.FunctionCall functionCall = new com.agui.types.FunctionCall(
                            tool.name(),
                            tool.parameters() != null ? tool.parameters().toString() : "{}"
                        );
                        
                        com.agui.types.ToolCall toolCall = new com.agui.types.ToolCall(
                            toolCallId,
                            "function",
                            functionCall
                        );
                        toolCalls.add(toolCall);
                    }
                    assistantMessage.setToolCalls(toolCalls);
                }
                
                // Add the assistant message to the agent's message state
                this.addMessage(assistantMessage);

                // Note: MessagesSnapshot is already sent at the beginning for frontend initialization
                // No need to send it again here as per AG-UI standard

                // Finish processing step
                StepFinishedEvent stepFinished = new StepFinishedEvent();
                stepFinished.setStepName("process_question");
                emitEvent(stepFinished, eventHandler);

                // End the run
                RunFinishedEvent runFinished = new RunFinishedEvent();
                runFinished.setThreadId(this.threadId);
                runFinished.setRunId(input.runId());
                runFinished.setResult(Map.of(
                        "ok", true,
                        "messageCount", this.messages.size(),
                        "toolCallCount", input.tools() != null ? input.tools().size() : 1
                ));
                emitEvent(runFinished, eventHandler);

            } catch (Exception e) {
                // Handle errors according to AG-UI standard
                RunErrorEvent runError = new RunErrorEvent();
                runError.setMessage("Error occurred during processing: " + e.getMessage());
                runError.setCode("PROCESSING_ERROR");
                emitEvent(runError, eventHandler);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Generate a response based on the user's question
     * This simulates an AI agent's response generation
     */
    private String generateResponse(String question) {
        if (question == null || question.trim().isEmpty()) {
            return "您好！我是AG-UI标准的AI助手。请告诉我您需要什么帮助？";
        }

        String lowerQuestion = question.toLowerCase();

        if (lowerQuestion.contains("你好") || lowerQuestion.contains("hello")) {
            return "您好！很高兴为您服务。我是基于AG-UI标准实现的AI助手，可以回答您的问题并提供帮助。";
        } else if (lowerQuestion.contains("帮助") || lowerQuestion.contains("help")) {
            return "我可以帮助您：\n1. 回答问题\n2. 提供信息\n3. 执行工具调用\n4. 进行对话交流\n\n请告诉我您的具体需求。";
        } else if (lowerQuestion.contains("ag-ui") || lowerQuestion.contains("agui")) {
            return "AG-UI是一个标准化的AI代理协议，它定义了AI系统与前端应用之间的通信标准。我们的实现遵循了官方规范，包括：\n\n• RUN_STARTED - 运行开始\n• TEXT_MESSAGE_CONTENT - 文本消息流\n• TOOL_CALL_START/END - 工具调用流程\n• RUN_FINISHED - 运行完成\n\n这个协议确保了AI系统与前端应用之间的标准化通信。";
        } else if (lowerQuestion.contains("时间") || lowerQuestion.contains("time")) {
            return "当前时间是：" + new Date().toString() + "\n\n我是实时响应的AI助手，可以为您提供准确的时间信息。";
        } else {
            return "我正在基于AG-UI标准处理您的请求。这是一个模拟的AI响应，展示了标准的AG-UI事件流程。在实际应用中，这里会连接到真实的AI模型。\n\n我可以帮助您解决各种问题，请告诉我您的具体需求。";
        }
    }
}
