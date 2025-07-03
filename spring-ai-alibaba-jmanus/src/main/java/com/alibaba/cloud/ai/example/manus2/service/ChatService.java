package com.alibaba.cloud.ai.example.manus2.service;

import com.alibaba.cloud.ai.example.manus2.model.*;
import com.alibaba.cloud.ai.example.manus2.model.enums.PromptType;
import com.alibaba.cloud.ai.example.manus2.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatService {

    private final CoreService coreService;
    private final StreamingService streamingService;
    private final CommonUtil commonUtil;

    @Value("${app.enable-tracing:false}")
    private boolean enableTracing;

    @Value("${app.start-turn-with-start-agent:false}")
    private boolean startTurnWithStartAgent;

    public ChatService(com.alibaba.cloud.ai.example.manus2.service.CoreService coreService, StreamingService streamingService, CommonUtil commonUtil) {
        this.coreService = coreService;
        this.streamingService = streamingService;
        this.commonUtil = commonUtil;
    }

    public ChatResponse processChatRequest(ChatRequest request) {
        log.info("Processing chat request with start agent: {}", request.getStartAgent());

        try {
            // Filter out agent transfer messages
            List<Message> inputMessages = coreService.filterAgentTransferMessages(request.getMessages());

            // Preprocess messages
            inputMessages = coreService.preprocessMessages(inputMessages);

            // Set system message
            inputMessages = coreService.setSysMessage(inputMessages);

            // Add sender details
            inputMessages = coreService.addSenderDetailsToMessages(inputMessages);

            // Check if this is a greeting turn
            boolean isGreetingTurn = inputMessages.stream()
                    .allMatch(msg -> "system".equals(msg.getRole()));

            if (isGreetingTurn) {
                return handleGreetingTurn(request);
            }

            // Process regular turn using streaming
            return processRegularTurnStreamed(request, inputMessages);

        } catch (Exception e) {
            log.error("Error processing chat request: {}", e.getMessage(), e);
            return ChatResponse.builder()
                    .messages(Collections.emptyList())
                    .state(Collections.singletonMap("error", e.getMessage()))
                    .build();
        }
    }

    public Flux<StreamEvent> processChatRequestStreamed(ChatRequest request) {
        log.info("Processing streaming chat request with start agent: {}", request.getStartAgent());

        try {
            // Filter out agent transfer messages
            List<Message> inputMessages = coreService.filterAgentTransferMessages(request.getMessages());

            // Preprocess messages
            inputMessages = coreService.preprocessMessages(inputMessages);

            // Set system message
            inputMessages = coreService.setSysMessage(inputMessages);

            // Add sender details
            inputMessages = coreService.addSenderDetailsToMessages(inputMessages);

            // Check if this is a greeting turn
            boolean isGreetingTurn = inputMessages.stream()
                    .allMatch(msg -> "system".equals(msg.getRole()));

            if (isGreetingTurn) {
                return handleGreetingTurnStreamed(request);
            }

            // Process regular turn using streaming
            return streamingService.runTurnStreamed(
                    inputMessages,
                    request.getStartAgent(),
                    request.getAgents(),
                    request.getTools(),
                    request.getPrompts(),
                    startTurnWithStartAgent,
                    request.getState(),
                    request.getAdditionalProperties(),
                    request.getEnableTracing() != null ? request.getEnableTracing() : enableTracing
            );

        } catch (Exception e) {
            log.error("Error processing streaming chat request: {}", e.getMessage(), e);
            return Flux.error(e);
        }
    }

    /**
     * 获取问候提示词，如果没有则使用默认问候语
     */
    private ChatResponse handleGreetingTurn(ChatRequest request) {
        String greetingPrompt = getPromptByType(request.getPrompts(), PromptType.GREETING);
        if (greetingPrompt == null) {
            greetingPrompt = "How can I help you today?";
        }

        Message greetingMessage = coreService.createGreetingMessage(request.getStartAgent(), greetingPrompt);

        Map<String, Object> finalState = coreService.createFinalState(
                request.getStartAgent(),
                Collections.singletonList(greetingMessage)
        );

        return ChatResponse.builder()
                .messages(Collections.singletonList(greetingMessage))
                .state(finalState)
                .build();
    }

    private Flux<StreamEvent> handleGreetingTurnStreamed(ChatRequest request) {
        String greetingPrompt = getPromptByType(request.getPrompts(), PromptType.GREETING);
        if (greetingPrompt == null) {
            greetingPrompt = "How can I help you today?";
        }

        Message greetingMessage = coreService.createGreetingMessage(request.getStartAgent(), greetingPrompt);

        Map<String, Object> finalState = coreService.createFinalState(
                request.getStartAgent(),
                Collections.singletonList(greetingMessage)
        );

        return Flux.just(
                new StreamEvent("message", greetingMessage),
                new StreamEvent("done", finalState)
        );
    }

    private ChatResponse processRegularTurnStreamed(ChatRequest request, List<Message> inputMessages) {
        // For non-streaming requests, we'll collect all events and return the final response
        List<Message> messages = new ArrayList<>();
        Map<String, Object> finalState = new HashMap<>();

        try {
            streamingService.runTurnStreamed(
                    inputMessages,
                    request.getStartAgent(),
                    request.getAgents(),
                    request.getTools(),
                    request.getPrompts(),
                    startTurnWithStartAgent,
                    request.getState(),
                    request.getAdditionalProperties(),
                    request.getEnableTracing() != null ? request.getEnableTracing() : enableTracing
            ).collectList().block().forEach(event -> {
                if ("message".equals(event.getType())) {
                    messages.add((Message) event.getData());
                } else if ("done".equals(event.getType())) {
                    finalState.putAll((Map<String, Object>) event.getData());
                }
            });

            return ChatResponse.builder()
                    .messages(messages)
                    .state(finalState)
                    .build();

        } catch (Exception e) {
            log.error("Error in stream processing: {}", e.getMessage(), e);
            return ChatResponse.builder()
                    .messages(Collections.emptyList())
                    .state(Collections.singletonMap("error", e.getMessage()))
                    .build();
        }
    }

    private String getPromptByType(List<PromptConfig> promptConfigs, PromptType type) {
        if (promptConfigs == null) {
            return null;
        }

        return promptConfigs.stream()
                .filter(prompt -> type.equals(prompt.getType()))
                .findFirst()
                .map(PromptConfig::getContent)
                .orElse(null);
    }

    public boolean isAgentTransferMessage(Message msg) {
        return msg.isAgentTransferMessage();
    }
} 