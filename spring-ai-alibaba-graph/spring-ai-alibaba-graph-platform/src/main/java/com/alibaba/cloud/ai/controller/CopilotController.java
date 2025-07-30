package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.service.WorkflowService;
import com.alibaba.cloud.ai.workflow.WorkflowSchema;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Queue;

/**
 * Copilot控制器 - 智能工作流调整助手
 * 支持通过自然语言描述来创建、编辑和改进多Agent工作流
 * 
 * @author AI Assistant
 */
@RestController
@RequestMapping("/api/copilot")
public class CopilotController {
    
    private static final Logger logger = LoggerFactory.getLogger(CopilotController.class);
    
    private final ChatModel chatModel;
    private final WorkflowService workflowService;
    private final ObjectMapper objectMapper;
    private final String copilotPrompt;
    private final String betterMultiAgentPrompt;
    
    @Autowired
    public CopilotController(ChatModel chatModel, WorkflowService workflowService) {
        this.chatModel = chatModel;
        this.workflowService = workflowService;
        this.objectMapper = new ObjectMapper();
        this.copilotPrompt = loadCopilotPrompt();
        this.betterMultiAgentPrompt = loadBetterMultiAgentPrompt();
    }
    
    /**
     * 智能调整工作流
     * 支持通过自然语言描述进行以下操作：
     * 1. 创建多Agent系统
     * 2. 创建新Agent
     * 3. 编辑现有Agent
     * 4. 改进Agent指令
     * 5. 添加/编辑/删除工具
     * 6. 添加/编辑/删除提示词
     * 7. 其他工作流调整需求
     */
    @SneakyThrows
    @PostMapping("/adjust")
    public Map<String, Object> adjustWorkflow(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        String userRequest = (String) request.get("userRequest");
        String workflowId = (String) request.get("workflowId");

        if (userRequest == null || userRequest.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "用户请求不能为空");
            return response;
        }

        logger.info("收到Copilot请求: workflowId={}, request={}", workflowId, userRequest);

        // 获取当前工作流配置（如果存在）
        WorkflowSchema currentSchema = null;
        if (workflowId != null && !workflowId.trim().isEmpty()) {
            currentSchema = workflowService.getWorkflowSchema(workflowId);
            if (currentSchema == null) {
                response.put("success", false);
                response.put("message", "工作流不存在: " + workflowId);
                return response;
            }
        }

        // 调用LLM生成新的工作流配置
        WorkflowSchema newSchema = generateWorkflowSchema(userRequest, currentSchema);

        // 修复inputKey/outputKey映射
        fixeSchemaInputKeyOutputKey(newSchema);

        // 循环检测，看newSchema是否符合要求，如果不符合要求，则重新生成
        int optimizationAttempts = 0;
        final int maxOptimizationAttempts = 3;

//        betterWorkflowSchema(newSchema, userRequest);
        
        if (optimizationAttempts >= maxOptimizationAttempts) {
            logger.warn("工作流优化达到最大尝试次数，使用当前配置");
        }

        // 保存或更新工作流
        String finalWorkflowId = newSchema.getWorkflowId();
        if (currentSchema != null) {
            // 更新现有工作流
            workflowService.updateWorkflow(newSchema);
            logger.info("更新工作流: {}", finalWorkflowId);
        } else {
            // 注册新工作流
            workflowService.registerWorkflow(newSchema);
            logger.info("创建新工作流: {}", finalWorkflowId);
        }

        response.put("success", true);
        response.put("message", "工作流调整成功");
        response.put("workflowId", finalWorkflowId);
        response.put("workflowName", newSchema.getName());
        response.put("agentCount", newSchema.getAgents() != null ? newSchema.getAgents().size() : 0);
        response.put("edgeCount", newSchema.getEdges() != null ? newSchema.getEdges().size() : 0);
        response.put("schema", newSchema);
        response.put("optimizationAttempts", optimizationAttempts);

        return response;
    }

    /**
     * 验证WorkflowSchema是否满足要求
     */
    private boolean isValidWorkflowSchema(WorkflowSchema schema, String userRequest) throws Exception {
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        
        // 构建评估提示词
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(betterMultiAgentPrompt).append("\n\n");
        promptBuilder.append("## 当前WorkflowSchema:\n");
        promptBuilder.append(objectMapper.writeValueAsString(schema)).append("\n\n");
        promptBuilder.append("## 用户需求:\n");
        promptBuilder.append(userRequest).append("\n\n");
        promptBuilder.append("请根据上述评估标准，对当前WorkflowSchema进行评估。");
        promptBuilder.append("只输出评估结果，不要输出优化后的JSON。");
        
        // 调用LLM进行评估
        String response = chatClient
                .prompt(promptBuilder.toString())
                .call()
                .content();
        
        logger.debug("工作流评估响应: {}", response);
        
        // 解析评估结果
        return parseEvaluationResult(response);
    }
    
    /**
     * 优化WorkflowSchema
     */
    private WorkflowSchema betterWorkflowSchema(WorkflowSchema schema, String userRequest) throws Exception {
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        
        // 构建优化提示词
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(betterMultiAgentPrompt).append("\n\n");
        promptBuilder.append("## 当前WorkflowSchema:\n");
        promptBuilder.append(objectMapper.writeValueAsString(schema)).append("\n\n");
        promptBuilder.append("## 用户需求:\n");
        promptBuilder.append(userRequest).append("\n\n");
        promptBuilder.append("请根据评估标准，对当前WorkflowSchema进行优化。");
        promptBuilder.append("输出优化后的完整WorkflowSchema JSON，保持原有的workflowId。");
        
        // 调用LLM进行优化
        String response = chatClient
                .prompt(promptBuilder.toString())
                .call()
                .content();
        
        logger.debug("工作流优化响应: {}", response);
        
        // 解析优化后的JSON
        try {
            String jsonContent = extractJsonFromResponse(response);
            return objectMapper.readValue(jsonContent, WorkflowSchema.class);
        } catch (Exception e) {
            logger.error("解析优化后的工作流配置失败", e);
            throw new RuntimeException("无法解析优化后的工作流配置: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析评估结果
     */
    private boolean parseEvaluationResult(String response) {
        // 查找"是否需要优化"的结果
        String[] lines = response.split("\n");
        for (String line : lines) {
            if (line.contains("是否需要优化:")) {
                String result = line.substring(line.indexOf(":") + 1).trim();
                return "否".equals(result);
            }
        }
        
        // 如果没有找到明确的"否"，默认认为需要优化
        logger.warn("无法解析评估结果，默认需要优化");
        return false;
    }



    
    /**
     * 使用LLM生成工作流配置
     */
    private WorkflowSchema generateWorkflowSchema(String userRequest, WorkflowSchema currentSchema) throws Exception {
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        
        // 构建提示词
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(copilotPrompt).append("\n\n");
        
        // 添加Agent编辑指南
        promptBuilder.append("## Agent Creation and Editing Guide:\n");
        promptBuilder.append(loadCopilotEditAgentGuide()).append("\n\n");
        
        // 添加WorkflowSchema JSON结构定义
        promptBuilder.append("## WorkflowSchema JSON Structure:\n");
        promptBuilder.append(loadWorkflowSchemaDefinition()).append("\n\n");
        
        // 添加示例工作流
        promptBuilder.append("## Example Workflow:\n");
        promptBuilder.append(loadBlogWritingWorkflowExample()).append("\n\n");
        
        // 添加当前工作流配置（如果存在）
        if (currentSchema != null) {
            promptBuilder.append("## Current Workflow Configuration:\n");
            promptBuilder.append(objectMapper.writeValueAsString(currentSchema));
            promptBuilder.append("\n\n");
        }

        // 添加输出要求
        promptBuilder.append("## Output Requirements:\n");
        promptBuilder.append("Please generate a complete WorkflowSchema JSON that addresses the user's request. ");
        promptBuilder.append("The JSON must be valid and compatible with the WorkflowSchema structure shown above. ");
        promptBuilder.append("If modifying an existing workflow, preserve the workflowId and make only the necessary changes. ");
        promptBuilder.append("If creating a new workflow, generate a unique workflowId.\n\n");
        promptBuilder.append("Output only the JSON, no additional text or explanations.");
        
        // 调用LLM
        String response = chatClient
                .prompt(promptBuilder.toString())
                .user(userRequest)
                .call()
                .content();
        
        logger.debug("LLM响应: {}", response);
        
        // 解析JSON响应
        try {
            // 尝试提取JSON部分（如果LLM返回了额外的文本）
            String jsonContent = extractJsonFromResponse(response);
            return objectMapper.readValue(jsonContent, WorkflowSchema.class);
        } catch (Exception e) {
            logger.error("解析LLM响应失败", e);
            throw new RuntimeException("无法解析LLM生成的工作流配置: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从LLM响应中提取JSON内容
     */
    private String extractJsonFromResponse(String response) {
        // 查找JSON开始和结束的位置
        int startIndex = response.indexOf('{');
        int endIndex = response.lastIndexOf('}');
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1);
        }
        
        // 如果没有找到完整的JSON，返回原始响应
        return response;
    }
    
    /**
     * 加载Copilot提示词
     */
    @SneakyThrows
    private String loadCopilotPrompt() {
        ClassPathResource resource = new ClassPathResource("prompts/copilot_multi_agent.md");
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
    
    /**
     * 加载更好的Copilot提示词
     */
    @SneakyThrows
    private String loadBetterMultiAgentPrompt() {
        ClassPathResource resource = new ClassPathResource("prompts/better_multi_agent.md");
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
    
    /**
     * 加载WorkflowSchema JSON结构定义
     */
    @SneakyThrows
    private String loadWorkflowSchemaDefinition() {
        ClassPathResource resource = new ClassPathResource("prompts/workflow_schema.json");
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
    
    /**
     * 加载博客写作工作流示例
     */
    private String loadBlogWritingWorkflowExample() {
        try {
            ClassPathResource resource = new ClassPathResource("examples/workflow-with-output-constraints.json");
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.warn("无法加载博客写作工作流示例文件", e);
            return "// Blog writing workflow example not available";
        }
    }
    
    /**
     * 加载Agent编辑指南
     */
    private String loadCopilotEditAgentGuide() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/copilot_edit_agent.md");
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.warn("无法加载Agent编辑指南文件", e);
            return "// Agent editing guide not available";
        }
    }

    /**
     * 修复WorkflowSchema的inputKey/outputKey映射
     * 通过深度遍历确保数据流正确传递
     */
    private void fixeSchemaInputKeyOutputKey(WorkflowSchema schema) {
        if (schema.getAgents() == null || schema.getAgents().isEmpty() || 
            schema.getEdges() == null || schema.getEdges().isEmpty()) {
            logger.warn("WorkflowSchema缺少agents或edges，无法修复inputKey/outputKey映射");
            return;
        }

        // 创建agent映射，便于快速查找
        Map<String, WorkflowSchema.AgentConfig> agentMap = new HashMap<>();
        for (WorkflowSchema.AgentConfig agent : schema.getAgents()) {
            agentMap.put(agent.getAgentId(), agent);
        }

        // 创建边映射，按fromAgentId分组
        Map<String, List<WorkflowSchema.EdgeConfig>> fromAgentEdges = new HashMap<>();
        // 创建边映射，按toAgentId分组
        Map<String, List<WorkflowSchema.EdgeConfig>> toAgentEdges = new HashMap<>();
        
        for (WorkflowSchema.EdgeConfig edge : schema.getEdges()) {
            fromAgentEdges.computeIfAbsent(edge.getFromAgentId(), k -> new ArrayList<>()).add(edge);
            toAgentEdges.computeIfAbsent(edge.getToAgentId(), k -> new ArrayList<>()).add(edge);
        }

        // 找到起始agent（没有入边的agent）
        Set<String> toAgentIds = schema.getEdges().stream()
                .map(WorkflowSchema.EdgeConfig::getToAgentId)
                .filter(id -> !"END".equals(id))
                .collect(java.util.stream.Collectors.toSet());
        
        Set<String> fromAgentIds = schema.getEdges().stream()
                .map(WorkflowSchema.EdgeConfig::getFromAgentId)
                .filter(id -> !"START".equals(id))
                .collect(java.util.stream.Collectors.toSet());
        
        Set<String> startAgentIds = fromAgentIds.stream()
                .filter(id -> !toAgentIds.contains(id))
                .collect(java.util.stream.Collectors.toSet());

        if (startAgentIds.isEmpty()) {
            logger.warn("未找到起始agent，使用第一个agent作为起始点");
            startAgentIds.add(schema.getAgents().get(0).getAgentId());
        }

        // 深度优先遍历修复inputKey/outputKey映射
        Set<String> visited = new HashSet<>();
        for (String startAgentId : startAgentIds) {
            fixInputKeyOutputKeyRecursive(startAgentId, agentMap, fromAgentEdges, visited, "user_request");
        }

        logger.info("完成inputKey/outputKey映射修复");
    }

    /**
     * 递归修复inputKey/outputKey映射
     */
    private void fixInputKeyOutputKeyRecursive(String currentAgentId, 
                                             Map<String, WorkflowSchema.AgentConfig> agentMap,
                                             Map<String, List<WorkflowSchema.EdgeConfig>> fromAgentEdges,
                                             Set<String> visited,
                                             String previousOutputKey) {
        
//        if (visited.contains(currentAgentId)) {
//            return; // 避免循环依赖
//        }
//
//        visited.add(currentAgentId);
        WorkflowSchema.AgentConfig currentAgent = agentMap.get(currentAgentId);
        
        if (currentAgent == null) {
            logger.warn("Agent不存在: {}", currentAgentId);
            return;
        }

        // 修复当前agent的inputKeys
        if (previousOutputKey != null) {
            List<String> currentInputKeys = currentAgent.getInputKeys();
            if (currentInputKeys == null) {
                currentInputKeys = new ArrayList<>();
            }
            
            // 如果inputKeys中不包含previousOutputKey，则添加
            if (!currentInputKeys.contains(previousOutputKey)) {
                  currentInputKeys.add(previousOutputKey);
                  currentAgent.setInputKeys(currentInputKeys);
                logger.debug("为agent {} 添加inputKey: {} -> {}", 
                           currentAgentId, previousOutputKey, currentInputKeys);
            }
        }

        // 生成当前agent的outputKey（如果不存在）
         String currentOutputKey = currentAgent.getOutputKey();
        if (currentOutputKey == null || currentOutputKey.trim().isEmpty()) {
//            currentOutputKey = generateOutputKey(currentAgent);
//            currentAgent.setOutputKey(currentOutputKey);
//            logger.debug("为agent {} 生成outputKey: {}", currentAgentId, currentOutputKey);
        }

        // 处理当前agent的所有出边
        List<WorkflowSchema.EdgeConfig> outEdges = fromAgentEdges.get(currentAgentId);
        if (outEdges != null) {
            for (WorkflowSchema.EdgeConfig edge : outEdges) {
                String nextAgentId = edge.getToAgentId();
                
                // 对于条件边，确保所有toAgent使用相同的outputKey
                if (edge.getCondition() != null) {
                    // 条件边：一个fromAgent对应多个toAgent，使用相同的outputKey
                    fixInputKeyOutputKeyRecursive(nextAgentId, agentMap, fromAgentEdges, visited, currentOutputKey);
                } else {
                    // 普通边：直接传递
                    fixInputKeyOutputKeyRecursive(nextAgentId, agentMap, fromAgentEdges, visited, currentOutputKey);
                }
            }
        }
    }

    /**
     * 根据agent类型和名称生成合适的outputKey
     */
    private String generateOutputKey(WorkflowSchema.AgentConfig agent) {
        String agentName = agent.getName();
        String agentType = agent.getType();
        
        // 根据agent类型和名称生成描述性的outputKey
        if (agentName != null && !agentName.trim().isEmpty()) {
            // 将agent名称转换为小写并用下划线连接
            String key = agentName.toLowerCase()
                    .replaceAll("[^a-zA-Z0-9]", "_")
                    .replaceAll("_+", "_")
                    .replaceAll("^_|_$", "");
            
            // 根据agent类型添加后缀
            switch (agentType) {
                case "llm":
                    return key + "_response";
                case "react":
                    return key + "_result";
                case "react_with_human":
                    return key + "_decision";
                default:
                    return key + "_output";
            }
        }
        
        // 如果agent名称为空，使用agentId
        return agent.getAgentId() + "_output";
    }

    /**
     * 广度优先遍历修复inputKey/outputKey映射（备用方法）
     */
    private void fixInputKeyOutputKeyBFS(WorkflowSchema schema) {
        if (schema.getAgents() == null || schema.getAgents().isEmpty() || 
            schema.getEdges() == null || schema.getEdges().isEmpty()) {
            return;
        }

        // 创建agent映射
        Map<String, WorkflowSchema.AgentConfig> agentMap = new HashMap<>();
        for (WorkflowSchema.AgentConfig agent : schema.getAgents()) {
            agentMap.put(agent.getAgentId(), agent);
        }

        // 创建边映射
        Map<String, List<WorkflowSchema.EdgeConfig>> fromAgentEdges = new HashMap<>();
        for (WorkflowSchema.EdgeConfig edge : schema.getEdges()) {
            fromAgentEdges.computeIfAbsent(edge.getFromAgentId(), k -> new ArrayList<>()).add(edge);
        }

        // 找到起始agents
        Set<String> toAgentIds = schema.getEdges().stream()
                .map(WorkflowSchema.EdgeConfig::getToAgentId)
                .collect(java.util.stream.Collectors.toSet());
        
        Set<String> fromAgentIds = schema.getEdges().stream()
                .map(WorkflowSchema.EdgeConfig::getFromAgentId)
                .collect(java.util.stream.Collectors.toSet());
        
        Set<String> startAgentIds = fromAgentIds.stream()
                .filter(id -> !toAgentIds.contains(id))
                .collect(java.util.stream.Collectors.toSet());

        if (startAgentIds.isEmpty()) {
            startAgentIds.add(schema.getAgents().get(0).getAgentId());
        }

        // BFS遍历
        Queue<String> queue = new java.util.LinkedList<>(startAgentIds);
        Set<String> visited = new HashSet<>();
        Map<String, String> agentOutputKeys = new HashMap<>();

        while (!queue.isEmpty()) {
            String currentAgentId = queue.poll();
            
            if (visited.contains(currentAgentId)) {
                continue;
            }
            
            visited.add(currentAgentId);
            WorkflowSchema.AgentConfig currentAgent = agentMap.get(currentAgentId);
            
            if (currentAgent == null) {
                continue;
            }

            // 生成当前agent的outputKey
            String currentOutputKey = currentAgent.getOutputKey();
            if (currentOutputKey == null || currentOutputKey.trim().isEmpty()) {
                currentOutputKey = generateOutputKey(currentAgent);
                currentAgent.setOutputKey(currentOutputKey);
            }
            agentOutputKeys.put(currentAgentId, currentOutputKey);

            // 处理出边
            List<WorkflowSchema.EdgeConfig> outEdges = fromAgentEdges.get(currentAgentId);
            if (outEdges != null) {
                for (WorkflowSchema.EdgeConfig edge : outEdges) {
                    String nextAgentId = edge.getToAgentId();
                    
                    // 修复下一个agent的inputKey
                    WorkflowSchema.AgentConfig nextAgent = agentMap.get(nextAgentId);
                    if (nextAgent != null) {
                        nextAgent.setInputKey(currentOutputKey);
                    }
                    
                    // 将下一个agent加入队列
                    if (!visited.contains(nextAgentId)) {
                        queue.offer(nextAgentId);
                    }
                }
            }
        }
    }
} 