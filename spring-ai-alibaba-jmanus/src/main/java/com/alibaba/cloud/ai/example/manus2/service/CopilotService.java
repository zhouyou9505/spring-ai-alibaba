package com.alibaba.cloud.ai.example.manus2.service;

import com.alibaba.cloud.ai.example.manus2.model.DataSource;
import com.alibaba.cloud.ai.example.manus2.model.Message;
import com.alibaba.cloud.ai.example.manus2.model.context.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.ai.chat.client.ChatClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class CopilotService {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Value("classpath:copilot_edit_agent.md")
    private Resource editAgentInstructions;

    public CopilotService(
            @Qualifier("chatClient") ChatClient chatClient,
            ObjectMapper objectMapper
    ) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    public String getResponse(
            List<Message> messages,
            String workflowSchema,
            String currentWorkflowConfig,
            Context context,
            List<DataSource> dataSources,
            String instructions
    ) throws IOException {
        String contextPrompt = buildContextPrompt(context);
        String dataSourcesPrompt = buildDataSourcesPrompt(dataSources);
        String systemPrompt = instructions.replace("{workflow_schema}", workflowSchema);

        Message lastMessage = messages.get(messages.size() - 1);
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
                lastMessage.getContent()
        );

        return chatClient.prompt()
            .system(systemPrompt)
            .user(userContent)
            .call()
            .content();
    }

    @SneakyThrows
    private String buildContextPrompt(Context context) {
        if (context == null) {
            return "";
        }

        return switch (context.getType()) {
            case "agent" -> String.format("""
                    **NOTE**: The user is currently working on the following agent:
                    %s
                    """, ((AgentContext) context).getAgentName());
            case "prompt" -> String.format("""
                    **NOTE**: The user is currently working on the following prompt:
                    %s
                    """, ((PromptContext) context).getPromptName());
            case "tool" -> String.format("""
                    **NOTE**: The user is currently working on the following tool:
                    %s
                    """, ((ToolContext) context).getToolName());
            case "chat" -> String.format("""
                    **NOTE**: The user has just tested the following chat using the workflow above and has provided feedback / question below this json dump:
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
                    **NOTE**: The following data sources are available:
                    ```json
                    %s
                    ```
                    """, objectMapper.writeValueAsString(dataSources));
        } catch (Exception e) {
            log.error("Error building data sources prompt", e);
            return "";
        }
    }

    public String getEditAgentInstructions() throws IOException {
        return new String(editAgentInstructions.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
} 