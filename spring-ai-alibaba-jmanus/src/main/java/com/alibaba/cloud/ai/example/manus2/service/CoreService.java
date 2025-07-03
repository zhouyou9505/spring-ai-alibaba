package com.alibaba.cloud.ai.example.manus2.service;//package com.alibaba.cloud.ai.example.manus2.service;

import com.rowboat.agents.constants.Instructions2;
import com.alibaba.cloud.ai.example.manus2.model.*;
import com.alibaba.cloud.ai.example.manus2.model.enums.ResponseType;
import com.alibaba.cloud.ai.example.manus2.model.enums.OutputVisibility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CoreService {
    
    public List<Message> orderMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        List<Message> orderedMessages = new ArrayList<>();
        for (Message msg : messages) {
            // Filter out fields with null values
            Map<String, Object> filteredMsg = new HashMap<>();
            if (msg.getRole() != null) filteredMsg.put("role", msg.getRole());
            if (msg.getSender() != null) filteredMsg.put("sender", msg.getSender());
            if (msg.getContent() != null) filteredMsg.put("content", msg.getContent());
            if (msg.getCreatedAt() != null) filteredMsg.put("created_at", msg.getCreatedAt());
            if (msg.getTimestamp() != null) filteredMsg.put("timestamp", msg.getTimestamp());

            // Add other fields in alphabetical order
            // Note: In Java, we need to manually handle additional fields
            // This is a simplified version that handles the main fields

            // Create ordered message
            Message ordered = Message.builder()
                    .role((String) filteredMsg.get("role"))
                    .sender((String) filteredMsg.get("sender"))
                    .content((String) filteredMsg.get("content"))
                    .createdAt((LocalDateTime) filteredMsg.get("created_at"))
                    .timestamp((Long) filteredMsg.get("timestamp"))
                    .build();

            orderedMessages.add(ordered);
        }

        return orderedMessages;
    }

    public List<Message> sortMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        return messages.stream()
                .sorted(Comparator.comparing(Message::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    public List<Message> preprocessMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        return messages.stream()
                .filter(msg -> msg.getContent() != null && !msg.getContent().trim().isEmpty())
                .collect(Collectors.toList());
    }

    public List<Message> setSysMessage(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            messages = new ArrayList<>();
        }

        // Check if the first message is a system message with empty content
        if (!messages.isEmpty() && "system".equals(messages.get(0).getRole()) &&
            (messages.get(0).getContent() == null || messages.get(0).getContent().trim().isEmpty())) {
            messages.get(0).setContent("You are a helpful assistant.");
            log.info("Updated system message: {}", messages.get(0));
        }

        return messages;
    }

    public List<Message> addSenderDetailsToMessages(List<Message> messages) {
        if (messages == null) {
            return new ArrayList<>();
        }

        return messages.stream()
                .map(msg -> {
                    msg.setSender(msg.getSender() != null ? msg.getSender() : null);
                    if (msg.getSender() != null) {
                        msg.setContent("Sender agent: " + msg.getSender() + "\nContent: " + msg.getContent());
                    }
                    return msg;
                })
                .collect(Collectors.toList());
    }

    public List<Message> filterAgentTransferMessages(List<Message> messages) {
        if (messages == null) {
            return new ArrayList<>();
        }

        return messages.stream()
                .filter(msg -> !isAgentTransferMessage(msg))
                .collect(Collectors.toList());
    }

    public boolean isAgentTransferMessage(Message msg) {
        if (msg.getContent() == null) {
            return false;
        }

        String content = msg.getContent().toLowerCase();
        return content.contains("transfer_to_agent") ||
               content.contains("transfer to agent") ||
               content.contains("handoff");
    }

    public List<Message> appendMessages(List<Message> messages, List<Message> accumulatedMessages) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        if (accumulatedMessages == null) {
            return messages;
        }

        // Create a set of existing message contents for O(1) lookup
        Set<String> existingContents = messages.stream()
                .map(Message::getContent)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Append new messages that don't already exist, maintaining order
        for (Message msg : accumulatedMessages) {
            if (msg.getContent() != null && !existingContents.contains(msg.getContent())) {
                messages.add(msg);
                existingContents.add(msg.getContent());
            }
        }

        return messages;
    }

    public Message createGreetingMessage(String agentName, String greetingPrompt) {
        return Message.builder()
                .role("assistant")
                .sender(agentName)
                .content(greetingPrompt)
                .timestamp(System.currentTimeMillis())
                .responseType(ResponseType.EXTERNAL.getValue())
                .build();
    }

    public Map<String, Object> createFinalState(String lastAgentName, List<Message> messages) {
        Map<String, Object> state = new HashMap<>();
        state.put("last_agent_name", lastAgentName);
        state.put("turn_messages", messages);
        return state;
    }

    public List<AgentConfig> addChildTransferRelatedInstructionsToAgents(List<AgentConfig> agents) {
        if (agents == null) {
            return new ArrayList<>();
        }

        return agents.stream()
                .map(agent -> {
                    String currentInstructions = agent.getInstructions() != null ? agent.getInstructions() : "";
                    String newInstructions = currentInstructions + "\n\n" + "-".repeat(100) + "\n\n" + Instructions2.CHILD_TRANSFER_RELATED_INSTRUCTIONS;

                    return AgentConfig.builder()
                            .id(agent.getId())
                            .name(agent.getName())
                            .description(agent.getDescription())
                            .instructions(newInstructions)
                            .model(agent.getModel())
                            .outputVisibility(agent.getOutputVisibility())
                            .role(agent.getRole())
                            .tools(agent.getTools())
                            .connectedAgents(agent.getConnectedAgents())
                            .toolConfigs(agent.getToolConfigs())
                            .modelConfigs(agent.getModelConfigs())
                            .ragEnabled(agent.isRagEnabled())
                            .maxCallsPerTurn(agent.getMaxCallsPerTurn())
                            .maxTokensPerTurn(agent.getMaxTokensPerTurn())
                            .maxTokensPerResponse(agent.getMaxTokensPerResponse())
                            .temperature(agent.getTemperature())
                            .visible(agent.isVisible())
                            .metadata(agent.getMetadata())
                            .additionalProperties(agent.getAdditionalProperties())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<AgentConfig> addOpenaiRecommendedInstructionsToAgents(List<AgentConfig> agents) {
        if (agents == null) {
            return new ArrayList<>();
        }

        return agents.stream()
                .map(agent -> {
                    String newInstructions = Instructions2.RECOMMENDED_PROMPT_PREFIX + "\n\n" +
                            (agent.getInstructions() != null ? agent.getInstructions() : "");
                    return AgentConfig.builder()
                            .id(agent.getId())
                            .name(agent.getName())
                            .description(agent.getDescription())
                            .instructions(newInstructions)
                            .model(agent.getModel())
                            .outputVisibility(agent.getOutputVisibility())
                            .role(agent.getRole())
                            .tools(agent.getTools())
                            .connectedAgents(agent.getConnectedAgents())
                            .toolConfigs(agent.getToolConfigs())
                            .modelConfigs(agent.getModelConfigs())
                            .ragEnabled(agent.isRagEnabled())
                            .maxCallsPerTurn(agent.getMaxCallsPerTurn())
                            .maxTokensPerTurn(agent.getMaxTokensPerTurn())
                            .maxTokensPerResponse(agent.getMaxTokensPerResponse())
                            .temperature(agent.getTemperature())
                            .visible(agent.isVisible())
                            .metadata(agent.getMetadata())
                            .additionalProperties(agent.getAdditionalProperties())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public boolean checkInternalVisibility(AgentConfig currentAgent) {
        return OutputVisibility.INTERNAL.equals(currentAgent.getOutputVisibility());
    }
}