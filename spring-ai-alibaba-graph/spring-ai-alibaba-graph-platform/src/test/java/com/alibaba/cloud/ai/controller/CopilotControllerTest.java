package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.TestApplication;
import com.alibaba.cloud.ai.service.WorkflowService;
import com.alibaba.cloud.ai.workflow.WorkflowSchema;
import com.alibaba.fastjson.JSON;
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
    void testAdjustWorkflow_CreateNewSchema_Success() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("userRequest", "创建一个智能客服系统，包含问题分类Agent(问题分类包含分类tool)、数学的交给数据Agent，" +
                "翻译的交给翻译Agent，数据分析的交给数据分析Agent(用web_search工具)，数据分析最后生成报告的Agent(llm类型)。");

        WorkflowSchema result = copilotController.adjustWorkflow(request);
        System.out.println("测试结果:\n " + JSON.toJSONString(result));

        // 执行
        Map<String, Object> runResult = workflowController.runWorkflow(null, result,Map.of("user_request","帮我对2024的经济数据写一报告"));
        System.out.println("测试结果: " + runResult);
    }

    @Test
    void testAdjustWorkflow_RunExitingSchema_Success() throws Exception {
        String schema = "{\"agents\":[{\"agentId\":\"request_classifier\",\"config\":{\"maxIterations\":1},\"description\":\"将用户请求分类到特定类别\",\"inputKey\":\"user_request\",\"inputKeys\":[\"user_request\"],\"instructions\":\"你是一个请求分类器。分析用户请求并将其分类为以下类别之一：'math', 'translation', 'data_analysis'。你必须只回复类别名称，不要其他内容。有效回复：'math', 'translation', 'data_analysis'。\",\"model\":\"qwen-turbo\",\"name\":\"请求分类Agent\",\"options\":{\"controlType\":\"auto\",\"disabled\":false,\"locked\":false,\"maxRetries\":3,\"outputVisibility\":\"public\",\"ragK\":5,\"timeout\":30000,\"toggleAble\":true},\"outputKey\":\"request_category\",\"tools\":[{\"autoMock\":true,\"name\":\"classification_tool\"}],\"type\":\"llm\"},{\"agentId\":\"math_agent\",\"config\":{\"maxIterations\":5},\"description\":\"处理数学相关的问题\",\"inputKey\":\"user_request\",\"inputKeys\":[\"user_request\",\"request_category\"],\"instructions\":\"你是一个数学专家。处理数学问题并提供解决方案。返回数学问题的答案。\",\"model\":\"qwen-turbo\",\"name\":\"数学处理Agent\",\"options\":{\"controlType\":\"auto\",\"disabled\":false,\"locked\":false,\"maxRetries\":3,\"outputVisibility\":\"public\",\"ragK\":5,\"timeout\":30000,\"toggleAble\":true},\"outputKey\":\"math_solution\",\"tools\":[{\"autoMock\":true,\"name\":\"math_solver\"}],\"type\":\"react\"},{\"agentId\":\"translation_agent\",\"config\":{\"maxIterations\":5},\"description\":\"处理翻译相关的请求\",\"inputKey\":\"user_request\",\"inputKeys\":[\"user_request\",\"request_category\"],\"instructions\":\"你是一个翻译专家。处理翻译请求并将结果返回。\",\"model\":\"qwen-turbo\",\"name\":\"翻译Agent\",\"options\":{\"controlType\":\"auto\",\"disabled\":false,\"locked\":false,\"maxRetries\":3,\"outputVisibility\":\"public\",\"ragK\":5,\"timeout\":30000,\"toggleAble\":true},\"outputKey\":\"translated_text\",\"tools\":[{\"autoMock\":true,\"name\":\"translator\"}],\"type\":\"react\"},{\"agentId\":\"data_analysis_agent\",\"config\":{\"maxIterations\":5},\"description\":\"处理数据分析相关的请求\",\"inputKey\":\"user_request\",\"inputKeys\":[\"user_request\",\"request_category\"],\"instructions\":\"你是一个数据分析专家。处理数据分析请求并提供结果 如果没有足够的数据信息，使用工具上网查询资料。\",\"model\":\"qwen-turbo\",\"name\":\"数据分析Agent\",\"options\":{\"controlType\":\"auto\",\"disabled\":false,\"locked\":false,\"maxRetries\":3,\"outputVisibility\":\"public\",\"ragK\":5,\"timeout\":30000,\"toggleAble\":true},\"outputKey\":\"analysis_result\",\"tools\":[{\"autoMock\":false,\"description\":\"使用网络搜索工具获取信息\",\"name\":\"web_search\"}],\"type\":\"react\"},{\"agentId\":\"report_generator\",\"config\":{\"maxIterations\":1},\"description\":\"生成最终报告\",\"inputKey\":\"results\",\"inputKeys\":[\"results\",\"math_solution\",\"translated_text\",\"analysis_result\"],\"instructions\":\"你是一个报告生成专家。根据其他Agent提供的结果生成最终报告。\",\"model\":\"qwen-turbo\",\"name\":\"报告生成Agent\",\"options\":{\"controlType\":\"auto\",\"disabled\":false,\"locked\":false,\"maxRetries\":3,\"outputVisibility\":\"public\",\"ragK\":5,\"timeout\":30000,\"toggleAble\":true},\"outputKey\":\"final_report\",\"tools\":[],\"type\":\"llm\"}],\"description\":\"一个包含问题分类、数学处理、翻译、数据分析和报告生成的多代理系统。\",\"edges\":[{\"edgeId\":\"edge1\",\"edgeType\":\"SEQUENTIAL\",\"fromAgentId\":\"START\",\"label\":\"分类请求\",\"toAgentId\":\"request_classifier\"},{\"condition\":{\"request_category\":\"math\"},\"edgeId\":\"edge2\",\"edgeType\":\"CONDITIONAL\",\"fromAgentId\":\"request_classifier\",\"label\":\"数学问题\",\"toAgentId\":\"math_agent\"},{\"condition\":{\"request_category\":\"translation\"},\"edgeId\":\"edge3\",\"edgeType\":\"CONDITIONAL\",\"fromAgentId\":\"request_classifier\",\"label\":\"翻译问题\",\"toAgentId\":\"translation_agent\"},{\"condition\":{\"request_category\":\"data_analysis\"},\"edgeId\":\"edge4\",\"edgeType\":\"CONDITIONAL\",\"fromAgentId\":\"request_classifier\",\"label\":\"数据分析问题\",\"toAgentId\":\"data_analysis_agent\"},{\"edgeId\":\"edge5\",\"edgeType\":\"SEQUENTIAL\",\"fromAgentId\":\"math_agent\",\"label\":\"生成报告\",\"toAgentId\":\"report_generator\"},{\"edgeId\":\"edge6\",\"edgeType\":\"SEQUENTIAL\",\"fromAgentId\":\"translation_agent\",\"label\":\"生成报告\",\"toAgentId\":\"report_generator\"},{\"edgeId\":\"edge7\",\"edgeType\":\"SEQUENTIAL\",\"fromAgentId\":\"data_analysis_agent\",\"label\":\"生成报告\",\"toAgentId\":\"report_generator\"},{\"edgeId\":\"edge8\",\"edgeType\":\"SEQUENTIAL\",\"fromAgentId\":\"report_generator\",\"label\":\"结束\",\"toAgentId\":\"END\"}],\"globalConfig\":{\"maxRetries\":3,\"timeout\":60000,\"enableLogging\":true,\"enableMetrics\":true,\"maxLoopIterations\":5,\"parallelExecution\":false},\"metadata\":{\"author\":\"AI Assistant\",\"category\":\"customer service\",\"createdAt\":\"2024-01-01T00:00:00Z\",\"customFields\":{\"estimatedDuration\":\"10-20 minutes\",\"complexity\":\"medium\",\"requiredAgents\":5,\"agentTypes\":[\"llm\",\"react\"],\"features\":[\"Output Constraints\",\"Conditional Routing\",\"Structured Output\"]},\"lastUpdatedAt\":\"2024-01-01T00:00:00Z\",\"tags\":[\"customer service\",\"multi-agent system\",\"classification\",\"math\",\"translation\",\"data analysis\",\"report generation\"]},\"name\":\"智能客服系统\",\"version\":\"1.0.0\",\"workflowId\":\"smart_customer_service_system_12345\"}";

        // 执行
        Map<String, Object> result = workflowController.runWorkflow(null, JSON.parseObject(schema,WorkflowSchema.class),Map.of("user_request","帮我对2024的经济数据写一报告"));
        System.out.println("测试结果: " + result);


    }
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