package com.alibaba.cloud.ai.example.manus2.service;

import com.alibaba.cloud.ai.example.manus2.model.*;
import com.alibaba.cloud.ai.example.manus2.model.context.*;
import com.alibaba.cloud.ai.example.manus2.model.enums.ResponseType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.alibaba.cloud.ai.example.manus2.config.OpenAIConfig;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class StreamingService {
    private final ChatClient chatClient;
    private final OpenAIConfig openAIConfig;
    private final ObjectMapper objectMapper;
    private final ExecuteTurnService executeTurnService;
    private final CoreService coreService;
    private final ControlService controlService;


    @Value("classpath:copilot/copilot_multi_agent.md")
    private Resource multiAgentInstructions;

    @Value("classpath:copilot/copilot_edit_agent.md")
    private Resource editAgentInstructions;

    @Value("classpath:copilot/example_multi_agent_1.md")
    private Resource multiAgentExample;

    @Value("classpath:copilot/current_workflow.md")
    private Resource currentWorkflowPrompt;

    public StreamingService(
            @Qualifier("chatClient") ChatClient chatClient,
            ObjectMapper objectMapper,
            OpenAIConfig openAIConfig,
            ExecuteTurnService executeTurnService,
            CoreService coreService,
            ControlService controlService
    ) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.openAIConfig = openAIConfig;
        this.executeTurnService = executeTurnService;
        this.controlService = controlService;
        this.coreService = coreService;
    }

    public Flux<String> getStreamingResponse(
            List<Message> messages,
            String workflowSchema,
            String currentWorkflowConfig,
            Context context,
            List<DataSource> dataSources
    ) throws IOException {
        String contextPrompt = buildContextPrompt(context);
        String dataSourcesPrompt = buildDataSourcesPrompt(dataSources);
        String systemPrompt = buildSystemPrompt().replace("{workflow_schema}", workflowSchema);
        systemPrompt = systemPrompt.replace("{agent_model}", openAIConfig.getModel());

        Message lastMessage = messages.get(messages.size() - 1);
        String originalContent = lastMessage.getContent();
        String userContent = String.format("""
                        Context:
                        The current workflow config is:
                        ```
                        %s
                        ```
                        
                        %s
                        %s
                        
                        User: %s
                        """,
                currentWorkflowConfig,
                contextPrompt,
                dataSourcesPrompt,
                originalContent
        );

        // Create a new message list with the modified last message
        List<org.springframework.ai.chat.messages.Message> aiMessages = new ArrayList<>();
        aiMessages.add(new SystemMessage(systemPrompt));

        // Add all messages except the last one
        for (int i = 0; i < messages.size() - 1; i++) {
            Message msg = messages.get(i);
            aiMessages.add(msg.getRole().equals("user") ?
                    new UserMessage(msg.getContent()) :
                    new AssistantMessage(msg.getContent()));
        }

        // Add the modified last message
        aiMessages.add(new UserMessage(userContent));

        return chatClient.prompt()
                .messages(aiMessages)
                .stream()
                .content();
    }

    @SneakyThrows
    private String buildContextPrompt(Context context) {
        if (context == null) {
            return "";
        }

        return switch (context.getType()) {
            case "agent" -> String.format("""
            **注意**：用户当前正在处理以下代理（agent）：
            %s
            """, ((AgentContext) context).getAgentName());
            case "prompt" -> String.format("""
            **注意**：用户当前正在处理以下提示词（prompt）：
            %s
            """, ((PromptContext) context).getPromptName());
            case "tool" -> String.format("""
            **注意**：用户当前正在处理以下工具（tool）：
            %s
            """, ((ToolContext) context).getToolName());
            case "chat" -> String.format("""
            **注意**：用户刚刚使用上述工作流测试了以下对话（chat），并在此 JSON 数据下方提供了反馈或问题：
            ```json
            %s
            ```
            """, objectMapper.writeValueAsString(((ChatContext) context).getMessages()));
            default -> "";
        };
    }

    private String buildDataSourcesPrompt(List<DataSource> dataSources) {
        if (dataSources == null || dataSources.isEmpty()) {
            return "";
        }

        try {
            return String.format("""
                **注意**：以下是可用的数据源：
                ```json
                %s
                ```
                """, objectMapper.writeValueAsString(dataSources));
        } catch (Exception e) {
            log.error("构建数据源提示信息时出错", e);
            return "";
        }
    }

    @SneakyThrows
    private String buildSystemPrompt() {
        String multiAgentInstructions = new String(this.multiAgentInstructions.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String multiAgentExample = new String(this.multiAgentExample.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String currentWorkflow = new String(this.currentWorkflowPrompt.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        return String.join("\n\n",
                multiAgentInstructions,
                multiAgentExample,
                currentWorkflow
        );
    }


    public Flux<StreamEvent> runTurnStreamed(
            List<Message> messages,
            String startAgentName,
            List<AgentConfig> agentConfigs,
            List<ToolConfig> toolConfigs,
            List<PromptConfig> promptConfigs,
            boolean startTurnWithStartAgent,
            Map<String, Object> state,
            Map<String, Object> completeRequest,
            Boolean enableTracing) {

        return Flux.create(sink -> {
            try {
                log.info("=== Starting new turn ===");
                log.info("Starting agent: {}", startAgentName);

                // Initialize agents and tools
                List<AgentConfig> newAgents = executeTurnService.getAgents(agentConfigs, toolConfigs, completeRequest);
                newAgents = coreService.addChildTransferRelatedInstructionsToAgents(newAgents);
                newAgents = coreService.addOpenaiRecommendedInstructionsToAgents(newAgents);

                // Get last agent name
                Message latestAssistantMsg = controlService.getLatestAssistantMsg(messages);
                String lastAgentName = controlService.getLastAgentName(
                        state, agentConfigs, startAgentName, null, latestAssistantMsg, startTurnWithStartAgent);

                // Get current agent
                AgentConfig currentAgent = getAgentByName(lastAgentName, newAgents);
                List<String> externalTools = controlService.getExternalTools(toolConfigs);

                // Initialize tracking variables
                final Map<String, Integer> finalTokensUsed = new HashMap<>();
                finalTokensUsed.put("total", 0);
                finalTokensUsed.put("prompt", 0);
                finalTokensUsed.put("completion", 0);

                List<Message> accumulatedMessages = new ArrayList<>();
                Map<String, Integer> agentMessageCounts = new ConcurrentHashMap<>();
                Map<String, Integer> childCallCounts = new ConcurrentHashMap<>();
                List<AgentConfig> parentStack = new ArrayList<>();

                // Create a final copy of messages for the loop
                final List<Message> finalMessages = new ArrayList<>(messages);

                // Create a wrapper for tokensUsed to make it effectively final
                final Map<String, Integer> finalTokensUsedWrapper = finalTokensUsed;

                int iteration = 0;
                while (true) {
                    iteration++;
                    boolean isInternalAgent = coreService.checkInternalVisibility(currentAgent);

                    log.info("Iteration {} of turn loop", iteration);
                    log.info("Current agent: {} (internal: {})", currentAgent.getName(), isInternalAgent);
                    log.info("Parent stack: {}", parentStack.stream().map(AgentConfig::getName).toList());

                    // Add accumulated messages to message history
                    List<Message> updatedMessages = coreService.appendMessages(finalMessages, accumulatedMessages);

                    // Run the current agent (simplified version)
                    Flux<StreamEvent> events = runAgentStreamed(
                            currentAgent, updatedMessages, externalTools, finalTokensUsedWrapper, enableTracing);

                    // Collect all events from this agent's stream
                    List<StreamEvent> agentEvents = events.collectList().block();

                    // Process collected events
                    for (StreamEvent event : agentEvents) {
                        try {
                            String eventType = event.getType();
                            Object eventData = event.getData();

                            log.info("Processing event type: {}", eventType);

                            // Handle raw_response_event (like Python)
                            if ("raw_response_event".equals(eventType)) {
                                // Handle token usage statistics
                                if (eventData instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> eventMap = (Map<String, Object>) eventData;
                                    Object data = eventMap.get("data");
                                    if (data instanceof Map) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> dataMap = (Map<String, Object>) data;
                                        if ("response.completed".equals(dataMap.get("type"))) {
                                            Object response = dataMap.get("response");
                                            if (response instanceof Map) {
                                                @SuppressWarnings("unchecked")
                                                Map<String, Object> responseMap = (Map<String, Object>) response;
                                                Object usage = responseMap.get("usage");
                                                if (usage instanceof Map) {
                                                    @SuppressWarnings("unchecked")
                                                    Map<String, Object> usageMap = (Map<String, Object>) usage;
                                                    try {
                                                        finalTokensUsedWrapper.put("total", finalTokensUsedWrapper.getOrDefault("total", 0) +
                                                                ((Number) usageMap.get("total_tokens")).intValue());
                                                        finalTokensUsedWrapper.put("prompt", finalTokensUsedWrapper.getOrDefault("prompt", 0) +
                                                                ((Number) usageMap.get("input_tokens")).intValue());
                                                        finalTokensUsedWrapper.put("completion", finalTokensUsedWrapper.getOrDefault("completion", 0) +
                                                                ((Number) usageMap.get("output_tokens")).intValue());
                                                        log.info("Updated token usage: {}", finalTokensUsedWrapper);
                                                    } catch (Exception e) {
                                                        log.warn("Warning: Tokens used is likely not available for your chosen model: {}", e.getMessage());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                continue;
                            }

                            // Handle agent_updated_stream_event (like Python)
                            else if ("agent_updated_stream_event".equals(eventType)) {
                                if (eventData instanceof AgentTransferEvent) {
                                    AgentTransferEvent transferEvent = (AgentTransferEvent) eventData;
                                    handleAgentTransfer(transferEvent, currentAgent, parentStack, childCallCounts, sink);
                                    currentAgent = transferEvent.getNewAgent();
                                }
                                continue;
                            }

                            // Handle message events (like Python's message_output_item)
                            else if ("message".equals(eventType)) {
                                if (eventData instanceof Message) {
                                    Message message = (Message) eventData;
                                    sink.next(event);

                                    if (message.getRole() != null && !message.getRole().equals("tool")) {
                                        accumulatedMessages.add(message);

                                        // Update agent message count for external agents (like Python does)
                                        if (!isInternalAgent) {
                                            agentMessageCounts.put(currentAgent.getName(), 1);
                                            log.info("External agent {} sent a message, updated agentMessageCounts: {}",
                                                    currentAgent.getName(), agentMessageCounts);
                                        }
                                    }
                                }
                            }

                            // Handle run_item_stream_event (like Python)
                            else if ("run_item_stream_event".equals(eventType)) {
                                // Handle run_item_stream_event with its subtypes like Python
                                if (eventData instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> runItemEvent = (Map<String, Object>) eventData;
                                    Object item = runItemEvent.get("item");

                                    if (item instanceof Map) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> itemMap = (Map<String, Object>) item;
                                        String itemType = (String) itemMap.get("type");

                                        log.info("Processing run_item_stream_event with item type: {}", itemType);

                                        try {
                                            // Handle tool_call_item (like Python)
                                            if ("tool_call_item".equals(itemType)) {
                                                Object rawItem = itemMap.get("raw_item");
                                                if (rawItem instanceof Map) {
                                                    @SuppressWarnings("unchecked")
                                                    Map<String, Object> rawItemMap = (Map<String, Object>) rawItem;

                                                    // Check if it's a web search call (like Python)
                                                    if ("web_search_call".equals(rawItemMap.get("type"))) {
                                                        String callId = (String) rawItemMap.get("id");
                                                        if (callId == null) {
                                                            callId = UUID.randomUUID().toString();
                                                        }

                                                        // Create tool call message for web search
                                                        Message toolCallMsg = Message.builder()
                                                                .role("assistant")
                                                                .sender(currentAgent.getName())
                                                                .content(null)
                                                                .toolCalls(Arrays.asList(ToolCall.builder()
                                                                        .id(callId)
                                                                        .type("function")
                                                                        .function(ToolCall.Function.builder()
                                                                                .name("web_search")
                                                                                .arguments("{\"search_id\": \"" + callId + "\"}")
                                                                                .build())
                                                                        .build()))
                                                                .responseType(ResponseType.INTERNAL.getValue())
                                                                .build();

                                                        log.info("Condition for tool call matched in run_item_stream_event. Appending tool call message: {}", toolCallMsg);
                                                        sink.next(new StreamEvent("message", toolCallMsg));
                                                        accumulatedMessages.add(toolCallMsg);

                                                        // Create tool call output dummy message
                                                        Message toolCallOutputDummyMsg = Message.builder()
                                                                .role("tool")
                                                                .content("Web search completed.")
                                                                .toolCallId(callId)
                                                                .toolName("web_search")
                                                                .responseType(ResponseType.INTERNAL.getValue())
                                                                .build();

                                                        sink.next(new StreamEvent("message", toolCallOutputDummyMsg));
                                                        accumulatedMessages.add(toolCallOutputDummyMsg);
                                                    } else {
                                                        // Handle regular tool calls (like Python)
                                                        String toolName = (String) rawItemMap.get("name");
                                                        String arguments = (String) rawItemMap.get("arguments");
                                                        String callId = (String) rawItemMap.get("call_id");

                                                        Message toolCallMsg = Message.builder()
                                                                .role("assistant")
                                                                .sender(currentAgent.getName())
                                                                .content(null)
                                                                .toolCalls(Arrays.asList(ToolCall.builder()
                                                                        .id(callId)
                                                                        .type("function")
                                                                        .function(ToolCall.Function.builder()
                                                                                .name(toolName)
                                                                                .arguments(arguments)
                                                                                .build())
                                                                        .build()))
                                                                .responseType(ResponseType.INTERNAL.getValue())
                                                                .build();

                                                        log.info("Condition for tool call matched in run_item_stream_event. Appending tool call message: {}", toolCallMsg);
                                                        sink.next(new StreamEvent("message", toolCallMsg));
                                                        accumulatedMessages.add(toolCallMsg);

                                                        // Create tool call output dummy message
                                                        Message toolCallOutputDummyMsg = Message.builder()
                                                                .role("tool")
                                                                .content("Web search completed.")
                                                                .toolCallId(callId)
                                                                .toolName("web_search")
                                                                .responseType(ResponseType.INTERNAL.getValue())
                                                                .build();

                                                        sink.next(new StreamEvent("message", toolCallOutputDummyMsg));
                                                        accumulatedMessages.add(toolCallOutputDummyMsg);
                                                    }
                                                }
                                            }

                                            // Handle tool_call_output_item (like Python)
                                            else if ("tool_call_output_item".equals(itemType)) {
                                                Object rawItem = itemMap.get("raw_item");
                                                if (rawItem instanceof Map) {
                                                    @SuppressWarnings("unchecked")
                                                    Map<String, Object> rawItemMap = (Map<String, Object>) rawItem;

                                                    // Check if it's web search results (like Python)
                                                    if ("web_search_results".equals(rawItemMap.get("type"))) {
                                                        String callId = (String) rawItemMap.get("search_id");
                                                        if (callId == null) {
                                                            callId = (String) rawItemMap.get("id");
                                                        }
                                                        if (callId == null) {
                                                            callId = UUID.randomUUID().toString();
                                                        }

                                                        String output = String.valueOf(itemMap.get("output"));

                                                        Message toolCallOutputMsg = Message.builder()
                                                                .role("tool")
                                                                .content(output)
                                                                .toolCallId(callId)
                                                                .toolName("web_search")
                                                                .responseType(ResponseType.INTERNAL.getValue())
                                                                .build();

                                                        log.info("Condition for tool call output matched in run_item_stream_event. Appending tool call output message: {}", toolCallOutputMsg);
                                                        sink.next(new StreamEvent("message", toolCallOutputMsg));
                                                        accumulatedMessages.add(toolCallOutputMsg);
                                                    }
                                                }
                                            }

                                            // Handle web_search_call_item (like Python)
                                            else if ("web_search_call_item".equals(itemType) ||
                                                    (itemMap.containsKey("raw_item") &&
                                                            itemMap.get("raw_item") instanceof Map &&
                                                            "web_search_call".equals(((Map<String, Object>) itemMap.get("raw_item")).get("type")))) {

                                                Object rawItem = itemMap.get("raw_item");
                                                String callId = null;

                                                if (rawItem instanceof Map) {
                                                    @SuppressWarnings("unchecked")
                                                    Map<String, Object> rawItemMap = (Map<String, Object>) rawItem;
                                                    callId = (String) rawItemMap.get("id");
                                                }

                                                if (callId == null) {
                                                    callId = UUID.randomUUID().toString();
                                                }

                                                Message toolCallMsg = Message.builder()
                                                        .role("assistant")
                                                        .sender(currentAgent.getName())
                                                        .content(null)
                                                        .toolCalls(Arrays.asList(ToolCall.builder()
                                                                .id(callId)
                                                                .type("function")
                                                                .function(ToolCall.Function.builder()
                                                                        .name("web_search")
                                                                        .arguments("{\"search_id\": \"" + callId + "\"}")
                                                                        .build())
                                                                .build()))
                                                        .responseType(ResponseType.INTERNAL.getValue())
                                                        .build();

                                                log.info("Condition for tool call matched in run_item_stream_event. Appending tool call message: {}", toolCallMsg);
                                                sink.next(new StreamEvent("message", toolCallMsg));
                                                accumulatedMessages.add(toolCallMsg);

                                                Message toolCallOutputDummyMsg = Message.builder()
                                                        .role("tool")
                                                        .content("Web search completed.")
                                                        .toolCallId(callId)
                                                        .toolName("web_search")
                                                        .responseType(ResponseType.INTERNAL.getValue())
                                                        .build();

                                                sink.next(new StreamEvent("message", toolCallOutputDummyMsg));
                                                accumulatedMessages.add(toolCallOutputDummyMsg);
                                            }

                                            // Handle web_search_results_item (like Python)
                                            else if ("web_search_results_item".equals(itemType) ||
                                                    (itemMap.containsKey("raw_item") &&
                                                            itemMap.get("raw_item") instanceof Map &&
                                                            "web_search_results".equals(((Map<String, Object>) itemMap.get("raw_item")).get("type")))) {

                                                Object rawItem = itemMap.get("raw_item");
                                                String callId = null;

                                                if (rawItem instanceof Map) {
                                                    @SuppressWarnings("unchecked")
                                                    Map<String, Object> rawItemMap = (Map<String, Object>) rawItem;

                                                    // Try to get search_id from different locations (like Python)
                                                    if (rawItemMap.containsKey("search_id")) {
                                                        callId = (String) rawItemMap.get("search_id");
                                                    } else if (rawItemMap.containsKey("id")) {
                                                        callId = (String) rawItemMap.get("id");
                                                    }
                                                }

                                                if (callId == null) {
                                                    callId = UUID.randomUUID().toString();
                                                }

                                                // Get results from different locations (like Python)
                                                Object results = null;
                                                if (itemMap.containsKey("output")) {
                                                    results = itemMap.get("output");
                                                } else if (rawItem instanceof Map) {
                                                    @SuppressWarnings("unchecked")
                                                    Map<String, Object> rawItemMap = (Map<String, Object>) rawItem;
                                                    if (rawItemMap.containsKey("results")) {
                                                        results = rawItemMap.get("results");
                                                    }
                                                }

                                                String resultsStr = "";
                                                try {
                                                    if (results != null) {
                                                        // Simple JSON serialization (in real implementation, use ObjectMapper)
                                                        resultsStr = results.toString();
                                                    }
                                                } catch (Exception e) {
                                                    log.warn("Error serializing results: {}", e.getMessage());
                                                    resultsStr = String.valueOf(results);
                                                }

                                                Message toolCallOutputMsg = Message.builder()
                                                        .role("tool")
                                                        .content(resultsStr)
                                                        .toolCallId(callId)
                                                        .toolName("web_search")
                                                        .responseType(ResponseType.INTERNAL.getValue())
                                                        .build();

                                                log.info("Condition for tool call output matched in run_item_stream_event. Appending tool call output message: {}", toolCallOutputMsg);
                                                sink.next(new StreamEvent("message", toolCallOutputMsg));
                                                accumulatedMessages.add(toolCallOutputMsg);
                                            }

                                            // Handle message_output_item (like Python)
                                            else if ("message_output_item".equals(itemType)) {
                                                Object rawItem = itemMap.get("raw_item");
                                                String content = "";

                                                if (rawItem instanceof Map) {
                                                    @SuppressWarnings("unchecked")
                                                    Map<String, Object> rawItemMap = (Map<String, Object>) rawItem;

                                                    // Extract content (simplified version of Python's logic)
                                                    if (rawItemMap.containsKey("content")) {
                                                        Object contentObj = rawItemMap.get("content");
                                                        if (contentObj instanceof String) {
                                                            content = (String) contentObj;
                                                        } else if (contentObj instanceof List) {
                                                            // Handle content items like Python does
                                                            @SuppressWarnings("unchecked")
                                                            List<Map<String, Object>> contentItems = (List<Map<String, Object>>) contentObj;
                                                            for (Map<String, Object> contentItem : contentItems) {
                                                                if (contentItem.containsKey("text")) {
                                                                    content += contentItem.get("text");
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                // Determine message type and create message (like Python)
                                                boolean isInternal = coreService.checkInternalVisibility(currentAgent);
                                                String responseType = isInternal ? ResponseType.INTERNAL.getValue() : ResponseType.EXTERNAL.getValue();

                                                Message messageOutputItem = Message.builder()
                                                        .role("assistant")
                                                        .sender(currentAgent.getName())
                                                        .content(content)
                                                        .responseType(responseType)
                                                        .toolCalls(null)
                                                        .toolCallId(null)
                                                        .toolName(null)
                                                        .build();

                                                // Record agent response (like Python)
                                                if (messageOutputItem.getToolCalls() == null) {
                                                    agentMessageCounts.put(currentAgent.getName(), 1);
                                                }

                                                sink.next(new StreamEvent("message", messageOutputItem));
                                                messageOutputItem.setContent("Sender agent: " + currentAgent.getName() + "\nContent: " + content);
                                                accumulatedMessages.add(messageOutputItem);

                                                // Return to parent agent or end turn (like Python)
                                                if (isInternal && !parentStack.isEmpty()) {
                                                    // Create control transition tool call
                                                    String toolCallId = UUID.randomUUID().toString();
                                                    Message transitionMessage = Message.builder()
                                                            .role("assistant")
                                                            .sender(currentAgent.getName())
                                                            .content(null)
                                                            .toolCalls(Arrays.asList(ToolCall.builder()
                                                                    .id(toolCallId)
                                                                    .type("function")
                                                                    .function(ToolCall.Function.builder()
                                                                            .name("transfer_to_agent")
                                                                            .arguments("{\"assistant\": \"" + parentStack.get(parentStack.size() - 1).getName() + "\"}")
                                                                            .build())
                                                                    .build()))
                                                            .responseType(ResponseType.INTERNAL.getValue())
                                                            .build();

                                                    sink.next(new StreamEvent("message", transitionMessage));

                                                    // Create control transition response
                                                    Message transitionResponse = Message.builder()
                                                            .role("tool")
                                                            .content("{\"assistant\": \"" + parentStack.get(parentStack.size() - 1).getName() + "\"}")
                                                            .toolCallId(toolCallId)
                                                            .toolName("transfer_to_agent")
                                                            .responseType(ResponseType.INTERNAL.getValue())
                                                            .build();

                                                    sink.next(new StreamEvent("message", transitionResponse));

                                                    // Switch back to parent agent
                                                    currentAgent = parentStack.remove(parentStack.size() - 1);
                                                    continue;
                                                } else if (!isInternal) {
                                                    break;
                                                }
                                            }

                                        } catch (Exception e) {
                                            log.error("=== Error in run_item_stream_event handling ===");
                                            log.error("Error: {}", e.getMessage());
                                            log.error("Event type: {}", eventType);
                                            log.error("Event item type: {}", itemType);
                                            log.error("Event details:");
                                            log.error("Raw item: {}", itemMap.get("raw_item"));
                                            log.error("Traceback:", e);
                                            log.error("==================================================");
                                            throw e;
                                        }
                                    }
                                }
                            }

                        } catch (Exception e) {
                            log.error("Error processing stream event: {}", e.getMessage(), e);
                            sink.error(e);
                            return;
                        }
                    }

                    // Check if we should end the turn (like Python does)
                    if (!isInternalAgent && agentMessageCounts.containsKey(currentAgent.getName())) {
                        log.info("Ending turn for external agent: {} after sending message", currentAgent.getName());
                        break;
                    }

                    // Add a safety check to prevent infinite loops
                    if (iteration > 100) {
                        log.warn("Reached maximum iterations (100), ending turn to prevent infinite loop");
                        break;
                    }
                }

                // Set final state
                Map<String, Object> finalState = coreService.createFinalState(
                        currentAgent.getName(), accumulatedMessages);
                // Ensure tokens field order matches Python: total, prompt, completion
                Map<String, Integer> orderedTokens = new HashMap<>();
                orderedTokens.put("total", finalTokensUsedWrapper.getOrDefault("total", 0));
                orderedTokens.put("prompt", finalTokensUsedWrapper.getOrDefault("prompt", 0));
                orderedTokens.put("completion", finalTokensUsedWrapper.getOrDefault("completion", 0));
                finalState.put("tokens", orderedTokens);

                sink.next(new StreamEvent("done", finalState));
                sink.complete();

            } catch (Exception e) {
                log.error("Error in stream processing: {}", e.getMessage(), e);
                sink.error(e);
            }
        });
    }

    private Flux<StreamEvent> runAgentStreamed(AgentConfig agent, List<Message> messages,
                                               List<String> externalTools, Map<String, Integer> tokensUsed,
                                               Boolean enableTracing) {

        log.info("Initializing streaming client for agent: {}", agent.getName());

        // Initialize default parameters
        if (externalTools == null) {
            externalTools = new ArrayList<>();
        }

        log.info("Beginning streaming run using Spring AI streaming API");

        return Flux.create(sink -> {
            try {
                // Convert our messages to Spring AI format
                List<org.springframework.ai.chat.messages.Message> aiMessages = new ArrayList<>();

                // Add system message with agent instructions
                if (agent.getInstructions() != null) {
                    aiMessages.add(new org.springframework.ai.chat.messages.SystemMessage(agent.getInstructions()));
                }

                // Add conversation messages
                for (Message msg : messages) {
                    if (msg.getContent() != null) {
                        if ("user".equals(msg.getRole())) {
                            aiMessages.add(new org.springframework.ai.chat.messages.UserMessage(msg.getContent()));
                        } else if ("assistant".equals(msg.getRole())) {
                            aiMessages.add(new org.springframework.ai.chat.messages.AssistantMessage(msg.getContent()));
                        } else if ("system".equals(msg.getRole())) {
                            aiMessages.add(new org.springframework.ai.chat.messages.SystemMessage(msg.getContent()));
                        }
                    }
                }

                // Use Spring AI's streaming API directly
                String modelName = agent.getModel() != null ? agent.getModel() : "qwen-max-latest";
                log.info("Calling AI model: {} for agent: {} using Spring AI streaming", modelName, agent.getName());

                // Use the streaming API from OpenAIClient
                Flux<String> streamResponse = chatClient.prompt().messages(aiMessages).stream().content();

                // Subscribe to the stream and emit events
                StringBuilder accumulatedContent = new StringBuilder();

                streamResponse.subscribe(
                        // onNext - handle each chunk
                        chunk -> {
                            log.debug("Received chunk from AI model: {}", chunk);
                            accumulatedContent.append(chunk);
                        },
                        // onError
                        error -> {
                            log.error("Error in AI stream: {}", error.getMessage(), error);
                            sink.error(error);
                        },
                        // onComplete
                        () -> {
                            try {
                                // Emit raw_response_event with token usage (like Python)
                                Map<String, Object> usage = new HashMap<>();
                                usage.put("total_tokens", 150); // Mock values for now
                                usage.put("input_tokens", 100);
                                usage.put("output_tokens", 50);

                                Map<String, Object> response = new HashMap<>();
                                response.put("usage", usage);

                                Map<String, Object> data = new HashMap<>();
                                data.put("type", "response.completed");
                                data.put("response", response);

                                Map<String, Object> rawResponseEvent = new HashMap<>();
                                rawResponseEvent.put("type", "raw_response_event");
                                rawResponseEvent.put("data", data);

                                sink.next(new StreamEvent("raw_response_event", rawResponseEvent));

                                // Token usage is handled in the main loop when processing raw_response_event

                                // Emit message_output_item (like Python)
                                boolean isInternal = coreService.checkInternalVisibility(agent);
                                String responseType = isInternal ? ResponseType.INTERNAL.getValue() : ResponseType.EXTERNAL.getValue();

                                Message responseMessage = Message.builder()
                                        .role("assistant")
                                        .sender(agent.getName())
                                        .content(accumulatedContent.toString())
                                        .responseType(responseType)
                                        .toolCalls(null)
                                        .toolCallId(null)
                                        .toolName(null)
                                        .build();

                                // Emit the message event
                                sink.next(new StreamEvent("message", responseMessage));

                                // Check if agent has connected agents for potential transfer
                                if (agent.getConnectedAgents() != null && !agent.getConnectedAgents().isEmpty()) {
                                    // Emit agent_updated_stream_event (like Python)
                                    String targetAgentName = agent.getConnectedAgents().get(0);
                                    AgentConfig targetAgent = AgentConfig.builder()
                                            .name(targetAgentName)
                                            .maxCallsPerTurn(3) // Default value
                                            .build();

                                    AgentTransferEvent transferEvent = new AgentTransferEvent(targetAgent, "Agent transfer requested");
                                    sink.next(new StreamEvent("agent_updated_stream_event", transferEvent));
                                }

                                sink.complete();
                            } catch (Exception e) {
                                log.error("Error creating response message: {}", e.getMessage(), e);
                                sink.error(e);
                            }
                        }
                );

            } catch (Exception e) {
                log.error("Error during streaming run: {}", e.getMessage(), e);
                sink.error(e);
            }
        });
    }

    private void handleAgentTransfer(AgentTransferEvent transferEvent, AgentConfig currentAgent,
                                     List<AgentConfig> parentStack, Map<String, Integer> childCallCounts,
                                     reactor.core.publisher.FluxSink<StreamEvent> sink) {

        // Skip self-transfer
        if (currentAgent.getName().equals(transferEvent.getNewAgent().getName())) {
            log.info("Skipping agent transfer attempt: {} -> {} (self-transfer)",
                    currentAgent.getName(), transferEvent.getNewAgent().getName());
            return;
        }

        // Check call limits
        String parentChildKey = currentAgent.getName() + ":" + transferEvent.getNewAgent().getName();
        int currentCount = childCallCounts.getOrDefault(parentChildKey, 0);
        if (currentCount >= DEFAULT_MAX_CALLS_PER_PARENT_AGENT) {
            log.info("Skipping transfer from {} to {} (max calls reached from parent to child)",
                    currentAgent.getName(), transferEvent.getNewAgent().getName());
            return;
        }

        // Create transfer messages
        String toolCallId = UUID.randomUUID().toString();

        // Assistant message with tool call
        Message transferMessage = Message.builder()
                .role("assistant")
                .sender(currentAgent.getName())
                .toolCalls(Arrays.asList(ToolCall.builder()
                        .id(toolCallId)
                        .type("function")
                        .function(ToolCall.Function.builder()
                                .name("transfer_to_agent")
                                .arguments("{\"assistant\": \"" + transferEvent.getNewAgent().getName() + "\"}")
                                .build())
                        .build()))
                .responseType(ResponseType.INTERNAL.getValue())
                .build();

        sink.next(new StreamEvent("message", transferMessage));

        // Tool response
        Message toolResponse = Message.builder()
                .role("tool")
                .content("{\"assistant\": \"" + transferEvent.getNewAgent().getName() + "\"}")
                .toolCallId(toolCallId)
                .toolName("transfer_to_agent")
                .responseType(ResponseType.INTERNAL.getValue())
                .build();

        sink.next(new StreamEvent("message", toolResponse));

        // Update tracking
        if (coreService.checkInternalVisibility(transferEvent.getNewAgent())) {
            childCallCounts.put(parentChildKey, currentCount + 1);
            parentStack.add(currentAgent);
        }
    }

    private AgentConfig getAgentByName(String agentName, List<AgentConfig> agents) {
        return agents.stream()
                .filter(agent -> agentName.equals(agent.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Agent with name " + agentName + " not found"));
    }

    private Map<String, Object> messageToMap(Message message) {
        Map<String, Object> map = new HashMap<>();
        map.put("role", message.getRole());
        map.put("content", message.getContent());
        map.put("sender", message.getSender());
        return map;
    }

    private static final int DEFAULT_MAX_CALLS_PER_PARENT_AGENT = 3;
} 