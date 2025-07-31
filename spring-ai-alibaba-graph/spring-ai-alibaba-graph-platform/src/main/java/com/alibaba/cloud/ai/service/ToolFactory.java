package com.alibaba.cloud.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具工厂类，用于管理和创建各种工具
 * 
 * @author AI Assistant
 */
@Slf4j
public class ToolFactory {

    private SearchTool searchTool;

    public ToolFactory (SearchTool searchTool){
        this.searchTool = searchTool;
    }

    private static final Map<String, ToolCallback> registeredTools = new HashMap<>();
    private static ChatClient defaultChatClient;

    public static void registerTool(String name, ToolCallback tool) {
        registeredTools.put(name, tool);
        log.info("注册工具: {}", name);
    }

    public static void registerTool(String name, Object toolBean) {
        ToolCallback[] from = ToolCallbacks.from(toolBean);
        for (ToolCallback toolCallback : from){
            registeredTools.put("web_search", toolCallback);
            log.info("注册工具: {}", name);
        }
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





} 