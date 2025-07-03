package com.alibaba.cloud.ai.example.manus2.service;

import com.alibaba.cloud.ai.example.manus2.model.AgentConfig;
import com.alibaba.cloud.ai.example.manus2.model.Message;
import com.alibaba.cloud.ai.example.manus2.model.ToolConfig;
import com.alibaba.cloud.ai.example.manus2.model.enums.ControlType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 代理控制服务类
 * 负责管理代理之间的控制权转移和消息处理逻辑
 */
@Slf4j
@Service
public class ControlService {

    /**
     * 根据最后代理和控制类型获取当前代理名称
     * 
     * @param state 当前状态映射
     * @param agentConfigs 代理配置列表
     * @param startAgentName 起始代理名称
     * @param msgType 消息类型
     * @param latestAssistantMsg 最新的助手消息
     * @param startTurnWithStartAgent 是否以起始代理开始轮次
     * @return 当前代理名称
     */
    public String getLastAgentName(Map<String, Object> state, 
                                   List<AgentConfig> agentConfigs, 
                                   String startAgentName, 
                                   String msgType, 
                                   Message latestAssistantMsg, 
                                   boolean startTurnWithStartAgent) {
        
        // 获取默认的最后代理名称
        String defaultLastAgentName = (String) state.getOrDefault("last_agent_name", "");
        
        // 获取最后代理的配置
        AgentConfig lastAgentConfig = getAgentConfigByName(defaultLastAgentName, agentConfigs);
        
        // 获取特定代理数据
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> agentData = (List<Map<String, Object>>) state.getOrDefault("agent_data", List.of());
        Map<String, Object> specificAgentData = getAgentDataByName(defaultLastAgentName, agentData);
        
        // 特殊情况的覆盖逻辑
        log.info("根据最后代理和控制类型设置代理控制");
        
        String lastAgentName;
        
        if ("tool".equals(msgType)) {
            // 工具调用时保持当前代理
            lastAgentName = defaultLastAgentName;
            
            // 验证最后代理名称是否与最新助手消息的发送者匹配
            String sender = latestAssistantMsg != null ? latestAssistantMsg.getSender() : "";
            if (!lastAgentName.equals(sender)) {
                throw new IllegalStateException("在工具调用处理期间，最后代理名称与最新助手消息的发送者不匹配");
            }
            
        } else if (startTurnWithStartAgent) {
            // 以起始代理开始轮次
            lastAgentName = startAgentName;
            
        } else {
            // 根据控制类型决定代理控制权
            String controlType = lastAgentConfig != null ? 
                (String) lastAgentConfig.getAdditionalProperties().getOrDefault("controlType", ControlType.RETAIN.getValue()) : 
                ControlType.RETAIN.getValue();
            
            if (ControlType.PARENT_AGENT.getValue().equals(controlType)) {
                // 转移到父代理
                lastAgentName = specificAgentData != null ? 
                    (String) specificAgentData.get("most_recent_parent_name") : null;
                
                if (lastAgentName == null || lastAgentName.isEmpty()) {
                    log.info("最近的父代理为空，默认使用相同代理");
                    lastAgentName = defaultLastAgentName;
                }
                
            } else if (ControlType.START_AGENT.getValue().equals(controlType)) {
                // 转移到起始代理
                lastAgentName = startAgentName;
                
            } else {
                // 保持当前代理（RETAIN）
                lastAgentName = defaultLastAgentName;
            }
        }
        
        // 记录代理名称变化
        if (!defaultLastAgentName.equals(lastAgentName)) {
            log.info("由于控制设置，最后代理名称从 {} 更改为 {}", defaultLastAgentName, lastAgentName);
        }
        
        return lastAgentName;
    }

    /**
     * 根据名称获取代理配置
     * 
     * @param agentName 代理名称
     * @param agentConfigs 代理配置列表
     * @return 代理配置，如果未找到则返回null
     */
    private AgentConfig getAgentConfigByName(String agentName, List<AgentConfig> agentConfigs) {
        if (agentName == null || agentName.isEmpty() || agentConfigs == null) {
            return null;
        }
        
        return agentConfigs.stream()
                .filter(config -> agentName.equals(config.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据名称获取代理数据
     * 
     * @param agentName 代理名称
     * @param agentData 代理数据列表
     * @return 代理数据映射，如果未找到则返回null
     */
    private Map<String, Object> getAgentDataByName(String agentName, List<Map<String, Object>> agentData) {
        if (agentName == null || agentName.isEmpty() || agentData == null) {
            return null;
        }
        
        return agentData.stream()
                .filter(data -> agentName.equals(data.get("name")))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取最新的助手消息
     * 查找角色为assistant的最新消息
     * 
     * @param messages 消息列表
     * @return 最新的助手消息，如果未找到则返回null
     */
    public Message getLatestAssistantMsg(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        
        // 从后往前查找最新的assistant消息
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if ("assistant".equals(message.getRole())) {
                return message;
            }
        }
        
        return null;
    }

    /**
     * 获取最新的非助手消息
     * 查找最后一个助手消息之后的所有消息
     * 
     * @param messages 消息列表
     * @return 最新的非助手消息列表
     */
    public List<Message> getLatestNonAssistantMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        
        // 从后往前查找最后一个assistant消息的位置
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if ("assistant".equals(message.getRole())) {
                // 返回最后一个assistant消息之后的所有消息
                return messages.subList(i + 1, messages.size());
            }
        }
        
        // 如果没有找到assistant消息，返回所有消息
        return messages;
    }

    /**
     * 验证代理控制权转移的合法性
     * 
     * @param fromAgentName 源代理名称
     * @param toAgentName 目标代理名称
     * @param agentConfigs 代理配置列表
     * @return 是否可以进行控制权转移
     */
    public boolean canTransferControl(String fromAgentName, String toAgentName, List<AgentConfig> agentConfigs) {
        if (fromAgentName == null || toAgentName == null || fromAgentName.equals(toAgentName)) {
            return false;
        }
        
        // 检查目标代理是否存在且可见
        AgentConfig targetConfig = getAgentConfigByName(toAgentName, agentConfigs);
        if (targetConfig == null || !targetConfig.isVisible()) {
            return false;
        }
        
        return true;
    }

    /**
     * 获取代理的控制类型
     * 
     * @param agentName 代理名称
     * @param agentConfigs 代理配置列表
     * @return 控制类型，默认为RETAIN
     */
    public ControlType getAgentControlType(String agentName, List<AgentConfig> agentConfigs) {
        AgentConfig config = getAgentConfigByName(agentName, agentConfigs);
        if (config == null || config.getAdditionalProperties() == null) {
            return ControlType.RETAIN;
        }
        
        String controlTypeValue = (String) config.getAdditionalProperties().get("controlType");
        if (controlTypeValue == null) {
            return ControlType.RETAIN;
        }
        
        try {
            return ControlType.valueOf(controlTypeValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("未知的控制类型: {}，使用默认值RETAIN", controlTypeValue);
            return ControlType.RETAIN;
        }
    }

    /**
     * 检查消息是否为工具调用消息
     * 
     * @param message 消息对象
     * @return 是否为工具调用消息
     */
    public boolean isToolCallMessage(Message message) {
        return message != null && "tool".equals(message.getRole());
    }

    /**
     * 检查消息是否为助手消息
     * 
     * @param message 消息对象
     * @return 是否为助手消息
     */
    public boolean isAssistantMessage(Message message) {
        return message != null && "assistant".equals(message.getRole());
    }

    /**
     * 获取消息的发送者名称
     * 
     * @param message 消息对象
     * @return 发送者名称，如果未找到则返回空字符串
     */
    public String getMessageSender(Message message) {
        return message != null ? message.getSender() : "";
    }

    /**
     * 验证状态映射的完整性
     * 
     * @param state 状态映射
     * @return 状态是否有效
     */
    public boolean isValidState(Map<String, Object> state) {
        return state != null && state.containsKey("last_agent_name");
    }

    /**
     * 创建默认状态映射
     * 
     * @param lastAgentName 最后代理名称
     * @return 默认状态映射
     */
    public Map<String, Object> createDefaultState(String lastAgentName) {
        return Map.of(
            "last_agent_name", lastAgentName != null ? lastAgentName : "",
            "agent_data", List.of()
        );
    }

    public List<String> getExternalTools(List<ToolConfig> toolConfigs) {
        return toolConfigs.stream().map(ToolConfig::getName).toList();
    }
} 