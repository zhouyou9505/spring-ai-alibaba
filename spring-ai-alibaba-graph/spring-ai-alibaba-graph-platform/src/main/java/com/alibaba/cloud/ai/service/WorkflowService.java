package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.workflow.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流服务类，提供工作流管理的业务逻辑
 * 
 * @author AI Assistant
 */
@Slf4j
@Service
public class WorkflowService {

    private final FlowRunner flowRunner;
//    private final CodeGenerator codeGenerator;
    private final Map<String, WorkflowSchema> workflowStorage = new ConcurrentHashMap<>();

    private ChatModel chatModel;

    private SearchTool searchTool;

    public WorkflowService(ChatModel chatModel, SearchTool searchTool) {
        this.chatModel = chatModel;
        this.flowRunner = new FlowRunner(chatModel,searchTool);
    }

    /**
     * 注册工作流
     */
    @SneakyThrows
    public WorkflowRegistrationResult registerWorkflow(WorkflowSchema schema) {
        // 存储工作流配置
        workflowStorage.put(schema.getWorkflowId(), schema);

        // 注册到FlowRunner
        flowRunner.registerWorkflow(schema, chatModel);

        return new WorkflowRegistrationResult(true, "工作流注册成功", schema.getWorkflowId());
    }

    /**
     * 运行工作流
     */
    public WorkflowExecutionResult runWorkflow(String workflowId, Map<String, Object> input) {
        try {
            Map<String, Object> result = flowRunner.runWorkflow(workflowId, input);
            return new WorkflowExecutionResult(true, "工作流执行成功", workflowId, result);
        } catch (Exception e) {
            return new WorkflowExecutionResult(false, "工作流执行失败: " + e.getMessage(), workflowId, null);
        }
    }


    /**
     * 获取工作流配置
     */
    public WorkflowSchema getWorkflowSchema(String workflowId) {
        return workflowStorage.get(workflowId);
    }

    /**
     * 获取所有工作流ID
     */
    public List<String> getAllWorkflowIds() {
        return flowRunner.getRegisteredWorkflowIds();
    }

    /**
     * 检查工作流是否存在
     */
    public boolean isWorkflowExists(String workflowId) {
        return flowRunner.isWorkflowRegistered(workflowId) && workflowStorage.containsKey(workflowId);
    }

    /**
     * 移除工作流
     */
    public boolean removeWorkflow(String workflowId) {
        try {
            flowRunner.removeWorkflow(workflowId);
            workflowStorage.remove(workflowId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 更新工作流
     */
    public WorkflowRegistrationResult updateWorkflow(WorkflowSchema schema) {
        try {
            // 先移除旧的工作流
            removeWorkflow(schema.getWorkflowId());
            
            // 注册新的工作流
            return registerWorkflow(schema);
        } catch (Exception e) {
            return new WorkflowRegistrationResult(false, "工作流更新失败: " + e.getMessage(), schema.getWorkflowId());
        }
    }

    /**
     * 获取工作流统计信息
     */
    public Map<String, Object> getWorkflowStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalWorkflows", workflowStorage.size());
        stats.put("registeredWorkflows", flowRunner.getRegisteredWorkflowIds().size());
        stats.put("workflowIds", workflowStorage.keySet());
        return stats;
    }


    /**
     * 工作流注册结果
     */
    public class WorkflowRegistrationResult {
        private final boolean success;
        private final String message;
        private final String workflowId;

        public WorkflowRegistrationResult(boolean success, String message, String workflowId) {
            this.success = success;
            this.message = message;
            this.workflowId = workflowId;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getWorkflowId() { return workflowId; }
    }

    /**
     * 工作流执行结果
     */
    public static class WorkflowExecutionResult {
        private final boolean success;
        private final String message;
        private final String workflowId;
        private final Map<String, Object> result;

        public WorkflowExecutionResult(boolean success, String message, String workflowId, Map<String, Object> result) {
            this.success = success;
            this.message = message;
            this.workflowId = workflowId;
            this.result = result;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getWorkflowId() { return workflowId; }
        public Map<String, Object> getResult() { return result; }
    }

    /**
     * 代码生成结果
     */
//    public static class CodeGenerationResult {
//        private final boolean success;
//        private final String message;
//        private final String generatedCode;
//        private final String fileName;
//
//        public CodeGenerationResult(boolean success, String message, String generatedCode, String fileName) {
//            this.success = success;
//            this.message = message;
//            this.generatedCode = generatedCode;
//            this.fileName = fileName;
//        }
//
//        public boolean isSuccess() { return success; }
//        public String getMessage() { return message; }
//        public String getGeneratedCode() { return generatedCode; }
//        public String getFileName() { return fileName; }
//    }
} 