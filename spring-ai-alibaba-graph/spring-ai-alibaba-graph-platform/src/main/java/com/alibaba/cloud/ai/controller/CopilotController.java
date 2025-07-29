package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.service.WorkflowService;
import com.alibaba.cloud.ai.workflow.WorkflowSchema;
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
    
    @Autowired
    public CopilotController(ChatModel chatModel, WorkflowService workflowService) {
        this.chatModel = chatModel;
        this.workflowService = workflowService;
        this.objectMapper = new ObjectMapper();
        this.copilotPrompt = loadCopilotPrompt();
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
        
        // 循环检测，看newSchema是否符合要求，如果不符合要求，则重新生成
        // while (!isValidWorkflowSchema(newSchema)) {
        //     newSchema = greaterWorkflowSchema(newSchema);
        // }

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


        return response;
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
        
        // 添加用户请求
//        promptBuilder.append("## User Request:\n");
//        promptBuilder.append(userRequest);
//        promptBuilder.append("\n\n");
//
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
     * 获取默认的Copilot提示词
     */
    private String getDefaultCopilotPrompt() {
        return """
            # Multi-Agent Workflow Copilot
            
            You are a helpful co-pilot for building and deploying multi-agent systems. Your goal is to perform tasks for the customer in designing a robust multi-agent system.
            
            ## Supported Tasks:
            1. Create a multi-agent system
            2. Create a new agent
            3. Edit an existing agent
            4. Improve an existing agent's instructions
            5. Adding / editing / removing tools
            6. Adding / editing / removing prompts
            7. Optimize workflow performance
            8. Add conditional logic and loops
            9. Configure parallel execution
            10. Any other workflow adjustments
            
            ## Agent Types:
            - llm: Language model agents
            - tool: Tool-calling agents
            - custom: Custom logic agents
            - condition: Conditional decision agents
            - simple: Simple processing agents
            - input: Input handling agents
            - output: Output handling agents
            
            ## Edge Types:
            - SEQUENTIAL: Sequential execution
            - CONDITIONAL: Conditional branching
            - LOOP: Loop execution
            - PARALLEL: Parallel execution
            
            ## Guidelines:
            - Always generate valid JSON compatible with WorkflowSchema structure
            - Preserve existing workflowId when modifying workflows
            - Generate unique workflowId for new workflows
            - Ensure all required fields are present
            - Use appropriate agent types and edge types
            - Include proper input/output mappings
            - Add meaningful descriptions and instructions
            - Consider workflow performance and scalability
            - Implement proper error handling and retry mechanisms
            - Support complex conditional logic and loops when needed
            """;
    }
} 