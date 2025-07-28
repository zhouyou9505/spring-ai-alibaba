package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.workflow.ToolConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.definition.DefaultToolDefinition;

/**
 * MockToolCallback - 模拟工具回调实现
 * 实现 ToolCallback 接口，内部使用 MockTool 调用 LLM
 * 
 * @author AI Assistant
 */
@Slf4j
public class MockToolCallback implements ToolCallback {

    private final MockTool mockTool;

    public MockToolCallback(MockTool mockTool) {
        this.mockTool = mockTool;
    }

    public MockToolCallback(ChatClient chatClient, String toolName, String toolDescription) {
        this.mockTool = new MockTool(chatClient, new ToolConfig(toolName, toolDescription));
    }

    public MockToolCallback(ChatClient chatClient, ToolConfig toolConfig) {
        this.mockTool = new MockTool(chatClient, toolConfig);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return DefaultToolDefinition.builder()
            .name(mockTool.getName())
            .description(mockTool.getDescription())
            .inputSchema("""
                {
                    "type": "object",
                    "properties": {
                        "input": {
                            "type": "string",
                            "description": "输入参数"
                        }
                    },
                    "required": ["input"]
                }
                """)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return mockTool.call(toolInput);
    }

    /**
     * 获取内部的 MockTool
     */
    public MockTool getMockTool() {
        return mockTool;
    }
} 