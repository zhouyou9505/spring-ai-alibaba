package com.alibaba.cloud.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具工厂类，用于管理和创建各种工具
 * 
 * @author AI Assistant
 */
@Slf4j
public class ToolFactory {
    
    private static final Map<String, ToolCallback> registeredTools = new HashMap<>();
    private static final ToolFactory instance = new ToolFactory();
    private static ChatClient defaultChatClient;

    public static void registerTool(String name, ToolCallback tool) {
        registeredTools.put(name, tool);
        log.info("注册工具: {}", name);
    }

    /**
     * 设置默认 ChatClient
     */
    public static void setDefaultChatClient(ChatClient chatClient) {
        defaultChatClient = chatClient;
    }

    /**
     * 获取工具
     */
    public static ToolCallback getTool(String toolName) {
        ToolCallback tool = registeredTools.get(toolName);
        if (tool == null) {
            log.warn("工具 {} 未找到，将使用 MockToolCallback", toolName);
            if (defaultChatClient != null) {
                return new MockToolCallback(defaultChatClient, toolName, "模拟工具: " + toolName);
            } else {
                log.error("默认 ChatClient 未设置，无法创建 MockToolCallback");
                return null;
            }
        }
        return tool;
    }
    

    /**
     * 获取实例
     */
    public static ToolFactory getInstance() {
        return instance;
    }

    /**
     * 注册默认工具
     */
    public static void registerDefaultTools(ChatClient chatClient) {
        setDefaultChatClient(chatClient);
        
        // 注册一些默认的模拟工具
        registerTool("mock_tool", new MockToolCallback(chatClient, "mock_tool", "模拟工具"));
        registerTool("web_search", new MockToolCallback(chatClient, "web_search", "网络搜索工具"));
        registerTool("database_query", new MockToolCallback(chatClient, "database_query", "数据库查询工具"));
        registerTool("content_generator", new MockToolCallback(chatClient, "content_generator", "内容生成工具"));
        registerTool("translation_service", new MockToolCallback(chatClient, "translation_service", "翻译服务工具"));
        registerTool("quality_checker", new MockToolCallback(chatClient, "quality_checker", "质量检查工具"));
        registerTool("grammar_checker", new MockToolCallback(chatClient, "grammar_checker", "语法检查工具"));
        registerTool("data_analysis", new MockToolCallback(chatClient, "data_analysis", "数据分析工具"));
        registerTool("chart_generator", new MockToolCallback(chatClient, "chart_generator", "图表生成工具"));
        registerTool("text_processor", new MockToolCallback(chatClient, "text_processor", "文本处理工具"));
        registerTool("language_detector", new MockToolCallback(chatClient, "language_detector", "语言检测工具"));
    }
} 