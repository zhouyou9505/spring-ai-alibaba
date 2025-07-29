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
        request.put("userRequest", "创建一个智能客服系统，包含接待Agent、问题分类Agent(问题分类包含分类tool)、数学的交给数据Agent，翻译的交给翻译Agent，数据分析的交给数据分析Agent，最后生成报告的Agent。");

        // 执行
        Map<String, Object> result = copilotController.adjustWorkflow(request);
        System.out.println("测试结果: " + result);
        
        // 验证基本结构
        assertNotNull(result);
        assertTrue(result.containsKey("success"));
        assertTrue(result.containsKey("message"));

//        workflowController.runWorkflow((String)result.get("workflowId"),Map.of("input", "今天天气"));

    }

    @Test
    void testAdjustWorkflow_UpdateExisting_Success() throws Exception {
        String schema = "{\n" +
                "  \"workflowId\": \"intelligent_customer_service_system\",\n" +
                "  \"name\": \"智能客服系统\",\n" +
                "  \"description\": \"一个包含接待、问题分类、数据处理、翻译、数据分析和报告生成的多Agent智能客服系统。\",\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"agents\": [\n" +
                "    {\n" +
                "      \"agentId\": \"reception_agent\",\n" +
                "      \"name\": \"接待Agent\",\n" +
                "      \"type\": \"llm\",\n" +
                "      \"description\": \"负责接待用户并收集基本信息，然后将控制权传递给问题分类Agent。\",\n" +
                "      \"instructions\": \"你是一个友好的接待员，请了解用户的需求并收集基本信息。然后将对话转交给问题分类Agent。\",\n" +
                "      \"model\": \"{agent_model}\",\n" +
                "      \"config\": {},\n" +
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
                "      \"agentId\": \"classification_agent\",\n" +
                "      \"name\": \"问题分类Agent\",\n" +
                "      \"type\": \"react\",\n" +
                "      \"description\": \"负责对用户的问题进行分类，并根据分类结果将问题传递给相应的处理Agent。\",\n" +
                "      \"instructions\": \"你是一个问题分类专家，使用分类工具对用户的问题进行分类。然后将问题转交给相应的处理Agent。\",\n" +
                "      \"model\": \"{agent_model}\",\n" +
                "      \"config\": {},\n" +
                "      \"inputKey\": \"reception_output\",\n" +
                "      \"outputKey\": \"classification_output\",\n" +
                "      \"options\": {\n" +
                "        \"toggleAble\": true,\n" +
                "        \"controlType\": \"auto\",\n" +
                "        \"outputVisibility\": \"internal\",\n" +
                "        \"maxRetries\": 3,\n" +
                "        \"timeout\": 30000\n" +
                "      },\n" +
                "      \"tools\": [\n" +
                "        {\n" +
                "          \"name\": \"problem_classifier\",\n" +
                "          \"autoMock\": true\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"agentId\": \"data_agent\",\n" +
                "      \"name\": \"数据Agent\",\n" +
                "      \"type\": \"react\",\n" +
                "      \"description\": \"负责处理数学相关的问题。\",\n" +
                "      \"instructions\": \"你是一个数学专家，负责解决数学相关的问题。请按照以下步骤工作：1. 理解问题 2. 解决问题 3. 返回答案\",\n" +
                "      \"model\": \"{agent_model}\",\n" +
                "      \"config\": {},\n" +
                "      \"inputKey\": \"classification_output\",\n" +
                "      \"outputKey\": \"data_output\",\n" +
                "      \"options\": {\n" +
                "        \"toggleAble\": true,\n" +
                "        \"controlType\": \"auto\",\n" +
                "        \"outputVisibility\": \"internal\",\n" +
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
                "      \"description\": \"负责处理翻译相关的问题。\",\n" +
                "      \"instructions\": \"你是一个翻译专家，负责解决翻译相关的问题。请按照以下步骤工作：1. 理解需要翻译的内容 2. 进行翻译 3. 返回翻译结果\",\n" +
                "      \"model\": \"{agent_model}\",\n" +
                "      \"config\": {},\n" +
                "      \"inputKey\": \"classification_output\",\n" +
                "      \"outputKey\": \"translation_output\",\n" +
                "      \"options\": {\n" +
                "        \"toggleAble\": true,\n" +
                "        \"controlType\": \"auto\",\n" +
                "        \"outputVisibility\": \"internal\",\n" +
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
                "      \"description\": \"负责处理数据分析相关的问题。\",\n" +
                "      \"instructions\": \"你是一个数据分析专家，负责解决数据分析相关的问题。请按照以下步骤工作：1. 理解数据需求 2. 选择合适的分析工具 3. 执行分析 4. 生成报告\",\n" +
                "      \"model\": \"{agent_model}\",\n" +
                "      \"config\": {},\n" +
                "      \"inputKey\": \"classification_output\",\n" +
                "      \"outputKey\": \"data_analysis_output\",\n" +
                "      \"options\": {\n" +
                "        \"toggleAble\": true,\n" +
                "        \"controlType\": \"auto\",\n" +
                "        \"outputVisibility\": \"internal\",\n" +
                "        \"maxRetries\": 3,\n" +
                "        \"timeout\": 30000\n" +
                "      },\n" +
                "      \"tools\": [\n" +
                "        {\n" +
                "          \"name\": \"data_processor\",\n" +
                "          \"autoMock\": true\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"chart_generator\",\n" +
                "          \"autoMock\": true\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"statistical_analyzer\",\n" +
                "          \"autoMock\": true\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"agentId\": \"report_generation_agent\",\n" +
                "      \"name\": \"报告生成Agent\",\n" +
                "      \"type\": \"reflect\",\n" +
                "      \"description\": \"负责生成最终的报告。\",\n" +
                "      \"instructions\": \"你是一个报告生成专家，负责整合所有处理结果并生成最终报告。请按照以下步骤工作：1. 收集所有处理结果 2. 整合信息 3. 生成报告 4. 返回报告\",\n" +
                "      \"model\": \"{agent_model}\",\n" +
                "      \"config\": {},\n" +
                "      \"inputKey\": \"data_output, translation_output, data_analysis_output\",\n" +
                "      \"outputKey\": \"final_report\",\n" +
                "      \"options\": {\n" +
                "        \"toggleAble\": true,\n" +
                "        \"controlType\": \"auto\",\n" +
                "        \"outputVisibility\": \"public\",\n" +
                "        \"maxRetries\": 3,\n" +
                "        \"timeout\": 30000\n" +
                "      },\n" +
                "      \"tools\": [\n" +
                "        {\n" +
                "          \"name\": \"report_generator\",\n" +
                "          \"autoMock\": true\n" +
                "        }\n" +
                "      ]\n" +
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
                "      \"toAgentId\": \"classification_agent\",\n" +
                "      \"label\": \"问题分类\",\n" +
                "      \"condition\": null,\n" +
                "      \"edgeType\": \"SEQUENTIAL\",\n" +
                "      \"config\": {}\n" +
                "    },\n" +
                "    {\n" +
                "      \"edgeId\": \"edge3\",\n" +
                "      \"fromAgentId\": \"classification_agent\",\n" +
                "      \"toAgentId\": \"data_agent\",\n" +
                "      \"label\": \"数据处理\",\n" +
                "      \"condition\": \"classification_output == '数学'\",\n" +
                "      \"edgeType\": \"CONDITIONAL\",\n" +
                "      \"config\": {}\n" +
                "    },\n" +
                "    {\n" +
                "      \"edgeId\": \"edge4\",\n" +
                "      \"fromAgentId\": \"classification_agent\",\n" +
                "      \"toAgentId\": \"translation_agent\",\n" +
                "      \"label\": \"翻译处理\",\n" +
                "      \"condition\": \"classification_output == '翻译'\",\n" +
                "      \"edgeType\": \"CONDITIONAL\",\n" +
                "      \"config\": {}\n" +
                "    },\n" +
                "    {\n" +
                "      \"edgeId\": \"edge5\",\n" +
                "      \"fromAgentId\": \"classification_agent\",\n" +
                "      \"toAgentId\": \"data_analysis_agent\",\n" +
                "      \"label\": \"数据分析\",\n" +
                "      \"condition\": \"classification_output == '数据分析'\",\n" +
                "      \"edgeType\": \"CONDITIONAL\",\n" +
                "      \"config\": {}\n" +
                "    },\n" +
                "    {\n" +
                "      \"edgeId\": \"edge6\",\n" +
                "      \"fromAgentId\": \"data_agent\",\n" +
                "      \"toAgentId\": \"report_generation_agent\",\n" +
                "      \"label\": \"生成报告\",\n" +
                "      \"condition\": null,\n" +
                "      \"edgeType\": \"SEQUENTIAL\",\n" +
                "      \"config\": {}\n" +
                "    },\n" +
                "    {\n" +
                "      \"edgeId\": \"edge7\",\n" +
                "      \"fromAgentId\": \"translation_agent\",\n" +
                "      \"toAgentId\": \"report_generation_agent\",\n" +
                "      \"label\": \"生成报告\",\n" +
                "      \"condition\": null,\n" +
                "      \"edgeType\": \"SEQUENTIAL\",\n" +
                "      \"config\": {}\n" +
                "    },\n" +
                "    {\n" +
                "      \"edgeId\": \"edge8\",\n" +
                "      \"fromAgentId\": \"data_analysis_agent\",\n" +
                "      \"toAgentId\": \"report_generation_agent\",\n" +
                "      \"label\": \"生成报告\",\n" +
                "      \"condition\": null,\n" +
                "      \"edgeType\": \"SEQUENTIAL\",\n" +
                "      \"config\": {}\n" +
                "    },\n" +
                "    {\n" +
                "      \"edgeId\": \"edge9\",\n" +
                "      \"fromAgentId\": \"report_generation_agent\",\n" +
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
                "    \"tags\": [\"customer service\", \"multi-agent\", \"workflow\"],\n" +
                "    \"category\": \"customer service\",\n" +
                "    \"customFields\": {\n" +
                "      \"estimatedDuration\": \"15-30 minutes\",\n" +
                "      \"complexity\": \"medium\",\n" +
                "      \"requiredAgents\": 6,\n" +
                "      \"agentTypes\": [\"llm\", \"react\", \"reflect\"]\n" +
                "    }\n" +
                "  }\n" +
                "}";

        // 执行
        Map<String, Object> result = workflowController.runWorkflow(null, JSON.parseObject(schema,WorkflowSchema.class),Map.of("user_message","帮我翻译：" +
                "你吃饭了吗"));
        System.out.println("测试结果: " + result);

        // 验证基本结构
        assertNotNull(result);
        assertTrue(result.containsKey("success"));
        assertTrue(result.containsKey("message"));
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