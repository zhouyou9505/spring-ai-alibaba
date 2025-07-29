package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.workflow.FlowRunner;
import com.alibaba.cloud.ai.workflow.WorkflowSchema;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流控制器，提供低代码平台的 REST API
 * 支持多Agent协作和复杂流程控制
 * 
 * @author AI Assistant
 */
@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {

    private final FlowRunner flowRunner;
//    private final CodeGenerator codeGenerator;

    @Autowired
    public WorkflowController(ChatModel chatModel) {
        this.flowRunner = new FlowRunner(chatModel);
//        this.codeGenerator = new CodeGenerator();
    }

    private ChatModel chatModel;

    /**
     * 注册工作流
     */
    @PostMapping("/register")
    public Map<String, Object> registerWorkflow(@RequestBody WorkflowSchema schema) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 注册工作流中的工具
            int totalTools = 0;
            if (schema.getAgents() != null) {
                for (WorkflowSchema.AgentConfig agent : schema.getAgents()) {
                    if (agent.getTools() != null) {
                        totalTools += agent.getTools().size();
                    }
                }
            }
            response.put("toolsRegistered", totalTools);
            
            flowRunner.registerWorkflow(schema,chatModel);
            response.put("success", true);
            response.put("message", "工作流注册成功");
            response.put("workflowId", schema.getWorkflowId());
            response.put("workflowName", schema.getName());
            response.put("agentCount", schema.getAgents() != null ? schema.getAgents().size() : 0);
            response.put("edgeCount", schema.getEdges() != null ? schema.getEdges().size() : 0);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "工作流注册失败: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
        }
        
        return response;
    }

    /**
     * 运行工作流
     */
    @PostMapping("/{workflowId}/run")
    public Map<String, Object> runWorkflow(@PathVariable String workflowId,WorkflowSchema workflowSchema,
                                          @RequestBody Map<String, Object> input) {
        Map<String, Object> response = new HashMap<>();

        if (workflowId == null){
            flowRunner.runWorkflow(workflowSchema,input);
        }else {
            flowRunner.runWorkflow(workflowId, input);
        }
        return response;
    }

    /**
     * 生成工作流代码
     */
    @PostMapping("/{workflowId}/generate-code")
    public Map<String, Object> generateCode(@PathVariable String workflowId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 从FlowRunner获取工作流配置
            WorkflowSchema schema = flowRunner.getWorkflowSchema(workflowId);
            if (schema == null) {
                response.put("success", false);
                response.put("message", "工作流未找到: " + workflowId);
                return response;
            }
            
//            String generatedCode = codeGenerator.generateControllerCode(schema);
            
            response.put("success", true);
            response.put("workflowId", workflowId);
            response.put("generatedCode", "");
            response.put("fileName", toCamelCase(schema.getName()) + "Controller.java");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "代码生成失败: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
        }
        
        return response;
    }

    /**
     * 获取已注册的工作流列表
     */
    @GetMapping("/list")
    public Map<String, Object> listWorkflows() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<String> workflowIds = flowRunner.getRegisteredWorkflowIds();
            response.put("success", true);
            response.put("workflows", workflowIds);
            response.put("count", workflowIds.size());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取工作流列表失败: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 检查工作流是否已注册
     */
    @GetMapping("/{workflowId}/status")
    public Map<String, Object> checkWorkflowStatus(@PathVariable String workflowId) {
        Map<String, Object> response = new HashMap<>();
        
        boolean isRegistered = flowRunner.isWorkflowRegistered(workflowId);
        response.put("workflowId", workflowId);
        response.put("registered", isRegistered);
        response.put("status", isRegistered ? "REGISTERED" : "NOT_FOUND");
        
        return response;
    }

    /**
     * 获取工作流统计信息
     */
    @GetMapping("/{workflowId}/stats")
    public Map<String, Object> getWorkflowStats(@PathVariable String workflowId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> stats = flowRunner.getWorkflowStats(workflowId);
            response.put("success", true);
            response.put("stats", stats);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取工作流统计信息失败: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 获取工作流配置
     */
    @GetMapping("/{workflowId}/config")
    public Map<String, Object> getWorkflowConfig(@PathVariable String workflowId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            WorkflowSchema schema = flowRunner.getWorkflowSchema(workflowId);
            if (schema == null) {
                response.put("success", false);
                response.put("message", "工作流未找到: " + workflowId);
                return response;
            }
            
            response.put("success", true);
            response.put("workflowId", workflowId);
            response.put("schema", schema);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取工作流配置失败: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 移除工作流
     */
    @DeleteMapping("/{workflowId}")
    public Map<String, Object> removeWorkflow(@PathVariable String workflowId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            flowRunner.removeWorkflow(workflowId);
            response.put("success", true);
            response.put("message", "工作流移除成功");
            response.put("workflowId", workflowId);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "工作流移除失败: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 获取平台信息
     */
    @GetMapping("/info")
    public Map<String, Object> getPlatformInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("platform", "Spring AI Alibaba Graph Platform");
        info.put("version", "1.0.0.4-SNAPSHOT");
        info.put("description", "低代码AI工作流平台 - 支持多Agent协作");
        info.put("features", new String[]{
            "多Agent协作工作流",
            "动态工作流注册",
            "基于NodeAction的Agent执行",
            "复杂条件分支支持",
            "循环流程控制",
            "代码自动生成",
            "REST API接口"
        });
        info.put("supportedAgentTypes", new String[]{
            "llm", "react", "react_with_human", "reflect"
        });
        info.put("supportedEdgeTypes", new String[]{
            "SEQUENTIAL", "CONDITIONAL", "LOOP", "PARALLEL"
        });
        
        return info;
    }

    /**
     * 转换为驼峰命名
     */
    private String toCamelCase(String str) {
        if (str == null || str.trim().isEmpty()) {
            return "Default";
        }
        
        String[] words = str.split("[\\s_-]+");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase());
            }
        }
        
        return result.toString();
    }
} 