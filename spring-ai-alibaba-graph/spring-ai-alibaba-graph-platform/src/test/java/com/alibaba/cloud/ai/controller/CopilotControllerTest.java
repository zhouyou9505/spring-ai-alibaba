package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.TestApplication;
import com.alibaba.cloud.ai.service.WorkflowService;
import com.alibaba.cloud.ai.workflow.WorkflowSchema;
import com.alibaba.fastjson.JSON;
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
        request.put("userRequest", "创建一个智能客服系统，包含接待Agent、问题分类Agent(问题分类包含分类tool)、数学的交给数据Agent，翻译的交给翻译Agent，数据分析的交给数据分析Agent，最后生成报告的Agent(llm类型)。");

        // 执行
        Map<String, Object> result = copilotController.adjustWorkflow(request);
        System.out.println("测试结果: " + result);
        

//        workflowController.runWorkflow((String)result.get("workflowId"),Map.of("input", "今天天气"));

    }

    @Test
    void testAdjustWorkflow_UpdateExisting_Success() throws Exception {
        String schema = "{\n" +
                "  \"workflowId\": \"smart_customer_service_system\",\n" +
                "  \"name\": \"智能客服系统\",\n" +
                "  \"description\": \"一个包含接待、问题分类、数据处理、翻译、数据分析和报告生成的多代理智能客服系统。\",\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"agents\": [\n" +
                "    {\n" +
                "      \"agentId\": \"reception_agent\",\n" +
                "      \"name\": \"接待Agent\",\n" +
                "      \"type\": \"llm\",\n" +
                "      \"description\": \"负责接待用户，收集基本信息并传递给问题分类Agent。\",\n" +
                "      \"instructions\": \"你是一个友好的接待员，请了解用户的需求并收集基本信息。提供清晰、简洁的回复。\",\n" +
                "      \"model\": \"qwen-turbo\",\n" +
                "      \"config\": {\n" +
                "        \"maxIterations\": 1\n" +
                "      },\n" +
                "      \"inputKey\": \"user_message\",\n" +
                "      \"outputKey\": \"reception_output\",\n" +
                "      \"options\": {\n" +
                "        \"toggleAble\": true,\n" +
                "        \"controlType\": \"auto\",\n" +
                "        \"outputVisibility\": \"public\",\n" +
                "        \"maxRetries\": 3,\n" +
                "        \"timeout\": 30000\n" +
                "      },\n" +
                "      \"tools\": []\n" +
                "    },\n" +
                "    {\n" +
                "      \"agentId\": \"request_classifier\",\n" +
                "      \"name\": \"问题分类Agent\",\n" +
                "      \"type\": \"react\",\n" +
                "      \"description\": \"将用户请求分类到特定类别，并调用相应的工具。\",\n" +
                "      \"instructions\": \"你是一个问题分类器。分析用户请求并将其分类为以下类别之一：'math', 'translation', 'data_analysis'。你必须只回复类别名称，不要其他内容。有效回复：'math', 'translation', 'data_analysis'。\",\n" +
                "      \"model\": \"qwen-turbo\",\n" +
                "      \"config\": {\n" +
                "        \"maxIterations\": 5\n" +
                "      },\n" +
                "      \"inputKey\": \"reception_output\",\n" +
                "      \"outputKey\": \"request_category\",\n" +
                "      \"options\": {\n" +
                "        \"toggleAble\": true,\n" +
                "        \"controlType\": \"auto\",\n" +
                "        \"outputVisibility\": \"public\",\n" +
                "        \"maxRetries\": 3,\n" +
                "        \"timeout\": 30000\n" +
                "      },\n" +
                "      \"tools\": [\n" +
                "        {\n" +
                "          \"name\": \"classification_tool\",\n" +
                "          \"autoMock\": false,\n" +
                "          \"description\": \"用于分类问题的工具\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"agentId\": \"math_agent\",\n" +
                "      \"name\": \"数学Agent\",\n" +
                "      \"type\": \"react\",\n" +
                "      \"description\": \"处理数学相关的问题。\",\n" +
                "      \"instructions\": \"你是一个数学专家。处理数学问题并提供解决方案。\",\n" +
                "      \"model\": \"qwen-turbo\",\n" +
                "      \"config\": {\n" +
                "        \"maxIterations\": 5\n" +
                "      },\n" +
                "      \"inputKey\": \"user_request\",\n" +
                "      \"outputKey\": \"math_solution\",\n" +
                "      \"options\": {\n" +
                "        \"toggleAble\": true,\n" +
                "        \"controlType\": \"auto\",\n" +
                "        \"outputVisibility\": \"public\",\n" +
                "        \"maxRetries\": 3,\n" +
                "        \"timeout\": 30000\n" +
                "      },\n" +
                "      \"tools\": [\n" +
                "        {\n" +
                "          \"name\": \"math_solver\",\n" +
                "          \"autoMock\": true\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"agentId\": \"translation_agent\",\n" +
                "      \"name\": \"翻译Agent\",\n" +
                "      \"type\": \"react\",\n" +
                "      \"description\": \"处理翻译相关的问题。\",\n" +
                "      \"instructions\": \"你是一个翻译专家。处理翻译问题并提供翻译结果。\",\n" +
                "      \"model\": \"qwen-turbo\",\n" +
                "      \"config\": {\n" +
                "        \"maxIterations\": 5\n" +
                "      },\n" +
                "      \"inputKey\": \"user_request\",\n" +
                "      \"outputKey\": \"translation_result\",\n" +
                "      \"options\": {\n" +
                "        \"toggleAble\": true,\n" +
                "        \"controlType\": \"auto\",\n" +
                "        \"outputVisibility\": \"public\",\n" +
                "        \"maxRetries\": 3,\n" +
                "        \"timeout\": 30000\n" +
                "      },\n" +
                "      \"tools\": [\n" +
                "        {\n" +
                "          \"name\": \"translator\",\n" +
                "          \"autoMock\": true\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"agentId\": \"data_analysis_agent\",\n" +
                "      \"name\": \"数据分析Agent\",\n" +
                "      \"type\": \"react\",\n" +
                "      \"description\": \"处理数据分析相关的问题。\",\n" +
                "      \"instructions\": \"你是一个数据分析专家。处理数据分析问题并提供分析结果。\",\n" +
                "      \"model\": \"qwen-turbo\",\n" +
                "      \"config\": {\n" +
                "        \"maxIterations\": 5\n" +
                "      },\n" +
                "      \"inputKey\": \"user_request\",\n" +
                "      \"outputKey\": \"analysis_result\",\n" +
                "      \"options\": {\n" +
                "        \"toggleAble\": true,\n" +
                "        \"controlType\": \"auto\",\n" +
                "        \"outputVisibility\": \"public\",\n" +
                "        \"maxRetries\": 3,\n" +
                "        \"timeout\": 30000\n" +
                "      },\n" +
                "      \"tools\": [\n" +
                "        {\n" +
                "          \"name\": \"data_analyzer\",\n" +
                "          \"autoMock\": true\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"agentId\": \"report_generator\",\n" +
                "      \"name\": \"报告生成Agent\",\n" +
                "      \"type\": \"llm\",\n" +
                "      \"description\": \"生成最终报告。\",\n" +
                "      \"instructions\": \"你是一个报告生成专家。根据接收到的结果生成最终报告。\",\n" +
                "      \"model\": \"qwen-turbo\",\n" +
                "      \"config\": {\n" +
                "        \"maxIterations\": 1\n" +
                "      },\n" +
                "      \"inputKey\": \"final_result\",\n" +
                "      \"outputKey\": \"report\",\n" +
                "      \"options\": {\n" +
                "        \"toggleAble\": true,\n" +
                "        \"controlType\": \"auto\",\n" +
                "        \"outputVisibility\": \"public\",\n" +
                "        \"maxRetries\": 3,\n" +
                "        \"timeout\": 30000\n" +
                "      },\n" +
                "      \"tools\": []\n" +
                "    }\n" +
                "  ],\n" +
                "  \"edges\": [\n" +
                "    {\n" +
                "      \"edgeId\": \"edge1\",\n" +
                "      \"fromAgentId\": \"START\",\n" +
                "      \"toAgentId\": \"reception_agent\",\n" +
                "      \"label\": \"开始\",\n" +
                "      \"condition\": null,\n" +
                "      \"edgeType\": \"SEQUENTIAL\",\n" +
                "      \"config\": {}\n" +
                "    },\n" +
                "    {\n" +
                "      \"edgeId\": \"edge2\",\n" +
                "      \"fromAgentId\": \"reception_agent\",\n" +
                "      \"toAgentId\": \"request_classifier\",\n" +
                "      \"label\": \"分类请求\",\n" +
                "      \"condition\": null,\n" +
                "      \"edgeType\": \"SEQUENTIAL\",\n" +
                "      \"config\": {}\n" +
                "    },\n" +
                "    {\n" +
                "      \"edgeId\": \"edge3\",\n" +
                "      \"fromAgentId\": \"request_classifier\",\n" +
                "      \"toAgentId\": \"math_agent\",\n" +
                "      \"label\": \"数学问题\",\n" +
                "      \"condition\": {\"request_category\": \"math\"},\n" +
                "      \"edgeType\": \"CONDITIONAL\",\n" +
                "      \"config\": {}\n" +
                "    },\n" +
                "    {\n" +
                "      \"edgeId\": \"edge4\",\n" +
                "      \"fromAgentId\": \"request_classifier\",\n" +
                "      \"toAgentId\": \"translation_agent\",\n" +
                "      \"label\": \"翻译问题\",\n" +
                "      \"condition\": {\"request_category\": \"translation\"},\n" +
                "      \"edgeType\": \"CONDITIONAL\",\n" +
                "      \"config\": {}\n" +
                "    },\n" +
                "    {\n" +
                "      \"edgeId\": \"edge5\",\n" +
                "      \"fromAgentId\": \"request_classifier\",\n" +
                "      \"toAgentId\": \"data_analysis_agent\",\n" +
                "      \"label\": \"数据分析问题\",\n" +
                "      \"condition\": {\"request_category\": \"data_analysis\"},\n" +
                "      \"edgeType\": \"CONDITIONAL\",\n" +
                "      \"config\": {}\n" +
                "    },\n" +
                "    {\n" +
                "      \"edgeId\": \"edge6\",\n" +
                "      \"fromAgentId\": \"math_agent\",\n" +
                "      \"toAgentId\": \"report_generator\",\n" +
                "      \"label\": \"生成报告\",\n" +
                "      \"condition\": null,\n" +
                "      \"edgeType\": \"SEQUENTIAL\",\n" +
                "      \"config\": {}\n" +
                "    },\n" +
                "    {\n" +
                "      \"edgeId\": \"edge7\",\n" +
                "      \"fromAgentId\": \"translation_agent\",\n" +
                "      \"toAgentId\": \"report_generator\",\n" +
                "      \"label\": \"生成报告\",\n" +
                "      \"condition\": null,\n" +
                "      \"edgeType\": \"SEQUENTIAL\",\n" +
                "      \"config\": {}\n" +
                "    },\n" +
                "    {\n" +
                "      \"edgeId\": \"edge8\",\n" +
                "      \"fromAgentId\": \"data_analysis_agent\",\n" +
                "      \"toAgentId\": \"report_generator\",\n" +
                "      \"label\": \"生成报告\",\n" +
                "      \"condition\": null,\n" +
                "      \"edgeType\": \"SEQUENTIAL\",\n" +
                "      \"config\": {}\n" +
                "    },\n" +
                "    {\n" +
                "      \"edgeId\": \"edge9\",\n" +
                "      \"fromAgentId\": \"report_generator\",\n" +
                "      \"toAgentId\": \"END\",\n" +
                "      \"label\": \"结束\",\n" +
                "      \"condition\": null,\n" +
                "      \"edgeType\": \"SEQUENTIAL\",\n" +
                "      \"config\": {}\n" +
                "    }\n" +
                "  ],\n" +
                "  \"globalConfig\": {\n" +
                "    \"maxRetries\": 3,\n" +
                "    \"timeout\": 60000,\n" +
                "    \"enableLogging\": true,\n" +
                "    \"enableMetrics\": true,\n" +
                "    \"maxLoopIterations\": 5,\n" +
                "    \"parallelExecution\": false\n" +
                "  },\n" +
                "  \"metadata\": {\n" +
                "    \"createdAt\": \"2024-01-01T00:00:00Z\",\n" +
                "    \"lastUpdatedAt\": \"2024-01-01T00:00:00Z\",\n" +
                "    \"author\": \"AI Assistant\",\n" +
                "    \"tags\": [\"customer-service\", \"multi-agent-system\", \"workflow\"],\n" +
                "    \"category\": \"customer-support\",\n" +
                "    \"customFields\": {\n" +
                "      \"estimatedDuration\": \"10-20 minutes\",\n" +
                "      \"complexity\": \"medium\",\n" +
                "      \"requiredAgents\": 6,\n" +
                "      \"agentTypes\": [\"llm\", \"react\"],\n" +
                "      \"features\": [\"Output Constraints\", \"Conditional Routing\", \"Structured Output\"]\n" +
                "    }\n" +
                "  }\n" +
                "}";

        // 执行
        Map<String, Object> result = workflowController.runWorkflow(null, JSON.parseObject(schema,WorkflowSchema.class),Map.of("user_request","帮我翻译：" +
                "你吃饭了吗"));
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