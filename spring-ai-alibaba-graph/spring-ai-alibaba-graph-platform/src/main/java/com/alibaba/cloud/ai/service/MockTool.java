package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.workflow.ToolConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;


/**
 * MockTool - 模拟工具实现
 * 提供方法调用 LLM 来生成模拟响应
 *
 * @author AI Assistant
 */
@Slf4j
public class MockTool extends ToolConfig{

    private final ChatClient chatClient;

    public MockTool(ChatClient chatClient, ToolConfig toolConfig) {
        super(toolConfig.getName(), toolConfig.getDescription(), toolConfig.getParameters());
        this.chatClient = chatClient;
        this.autoMock = toolConfig.isAutoMock();
    }

    /**
     * 调用 LLM 生成模拟响应
     */
    public String call(String toolInput) {
        // 构建提示词
        String prompt = buildPrompt(toolInput);

        // 调用 LLM
        return chatClient.prompt(prompt).messages().call().content();
    }

    /**
     * 构建提示词
     */
    private String buildPrompt(String toolInput) {
        return String.format("""
                你是一个模拟工具 '%s'，描述：%s ，你的参数是：%s。
                
                请根据以下输入生成一个合理的模拟响应：
                输入：%s
                
                要求：
                1. 响应应该符合工具的功能描述
                2. 返回 JSON 格式的结果
                3. 包含输入参数和模拟结果
                4. 响应要真实可信
                
                请生成响应：
                """, name, description, parameters, toolInput);
    }



}