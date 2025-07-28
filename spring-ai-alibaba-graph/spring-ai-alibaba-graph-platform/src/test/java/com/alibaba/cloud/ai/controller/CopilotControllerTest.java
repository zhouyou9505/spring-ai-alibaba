package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.TestApplication;
import com.alibaba.cloud.ai.service.WorkflowService;
import com.alibaba.cloud.ai.workflow.WorkflowSchema;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class CopilotControllerTest {

    @Autowired
    private CopilotController copilotController;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowController workflowController;

    @Test
    void testAdjustWorkflow_CreateNewSystem_Success() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("userRequest", "创建一个智能客服系统，包含接待Agent、问题分类Agent、技术支持Agent和满意度调查Agent");

        // 执行
        Map<String, Object> result = copilotController.adjustWorkflow(request);
        System.out.println("测试结果: " + result);
        
        // 验证基本结构
        assertNotNull(result);
        assertTrue(result.containsKey("success"));
        assertTrue(result.containsKey("message"));

        workflowController.runWorkflow((String)result.get("workflowId"),Map.of("input", "测试输入数据"));

    }
//
//    @Test
//    void testAdjustWorkflow_UpdateExisting_Success() throws Exception {
//        // 准备测试数据
//        WorkflowSchema existingSchema = new WorkflowSchema();
//        existingSchema.setWorkflowId("existing-workflow");
//        existingSchema.setName("现有工作流");
//
//        Map<String, Object> request = new HashMap<>();
//        request.put("userRequest", "在现有工作流中添加一个数据分析Agent，用于分析客户交互数据并生成报告");
//        request.put("workflowId", "existing-workflow");
//
//        // 执行
//        Map<String, Object> result = copilotController.adjustWorkflow(request);
//        System.out.println("测试结果: " + result);
//
//        // 验证基本结构
//        assertNotNull(result);
//        assertTrue(result.containsKey("success"));
//        assertTrue(result.containsKey("message"));
//    }
//
//    @Test
//    void testAdjustWorkflow_MissingRequest() {
//        // 准备测试数据
//        Map<String, Object> request = new HashMap<>();
//        request.put("workflowId", "existing-workflow");
//        // 故意不设置userRequest
//
//        // 执行
//        Map<String, Object> result = copilotController.adjustWorkflow(request);
//
//        // 验证
//        assertNotNull(result);
//        assertFalse((Boolean) result.get("success"));
//        assertEquals("用户请求不能为空", result.get("message"));
//    }
//
//    @Test
//    void testAdjustWorkflow_WorkflowNotFound() {
//        // 准备测试数据
//        Map<String, Object> request = new HashMap<>();
//        request.put("userRequest", "添加一个新的Agent到工作流");
//        request.put("workflowId", "non-existent-workflow");
//
//        // 执行
//        Map<String, Object> result = copilotController.adjustWorkflow(request);
//
//        // 验证
//        assertNotNull(result);
//        assertFalse((Boolean) result.get("success"));
//        assertEquals("工作流不存在: non-existent-workflow", result.get("message"));
//    }
//
//    @Test
//    void testAdjustWorkflow_ComplexRequest() throws Exception {
//        Map<String, Object> request = new HashMap<>();
//        request.put("userRequest", "创建一个技术博客写作工作流，参考博客写作示例，包含选题验证、内容创作、校对和发布功能");
//
//        // 执行
//        Map<String, Object> result = copilotController.adjustWorkflow(request);
//        System.out.println("复杂请求测试结果: " + result);
//
//        // 验证基本结构
//        assertNotNull(result);
//        assertTrue(result.containsKey("success"));
//        assertTrue(result.containsKey("message"));
//    }
//
//    @Test
//    void testAdjustWorkflow_PerformanceOptimization() throws Exception {
//        Map<String, Object> request = new HashMap<>();
//        request.put("userRequest", "优化技术支持工作流，添加并行处理能力，让问题分类Agent和知识库Agent可以同时工作，提高响应速度。同时添加重试机制和超时处理。");
//
//        // 执行
//        Map<String, Object> result = copilotController.adjustWorkflow(request);
//        System.out.println("性能优化测试结果: " + result);
//
//        // 验证基本结构
//        assertNotNull(result);
//        assertTrue(result.containsKey("success"));
//        assertTrue(result.containsKey("message"));
//    }
} 