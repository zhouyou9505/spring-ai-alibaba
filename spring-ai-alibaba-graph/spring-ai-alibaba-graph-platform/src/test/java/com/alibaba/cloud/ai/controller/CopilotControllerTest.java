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
    void testAdjustWorkflow_CreateNewSystem_Success() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("userRequest", "创建一个智能客服系统，包含问题分类Agent(问题分类包含分类tool)、数学的交给数据Agent，翻译的交给翻译Agent，数据分析的交给数据分析Agent，最后生成报告的Agent(llm类型)。");

        // 执行
        Map<String, Object> result = copilotController.adjustWorkflow(request);
        System.out.println("测试结果: " + result);
        

//        workflowController.runWorkflow((String)result.get("workflowId"),Map.of("input", "今天天气"));

    }

    @Test
    void testAdjustWorkflow_UpdateExisting_Success() throws Exception {
        String schema = "{\n" +
                "  \"agents\" : [ {\n" +
                "    \"agentId\" : \"reception_agent\",\n" +
                "    \"config\" : {\n" +
                "      \"maxIterations\" : 1\n" +
                "    },\n" +
                "    \"description\" : \"简单对话接待，收集用户需求\",\n" +
                "    \"inputKey\" : \"user_message\",\n" +
                "    \"inputKeys\" : [ \"user_message\", \"user_request\" ],\n" +
                "    \"instructions\" : \"你是一个友好的接待员，请了解用户的需求并收集基本信息。提供清晰、简洁的回复。\",\n" +
                "    \"model\" : \"qwen-turbo\",\n" +
                "    \"name\" : \"接待Agent\",\n" +
                "    \"options\" : {\n" +
                "      \"controlType\" : \"auto\",\n" +
                "      \"disabled\" : false,\n" +
                "      \"locked\" : false,\n" +
                "      \"maxRetries\" : 3,\n" +
                "      \"outputVisibility\" : \"public\",\n" +
                "      \"ragK\" : 5,\n" +
                "      \"timeout\" : 30000,\n" +
                "      \"toggleAble\" : true\n" +
                "    },\n" +
                "    \"outputKey\" : \"reception_output\",\n" +
                "    \"tools\" : [ ],\n" +
                "    \"type\" : \"llm\"\n" +
                "  }, {\n" +
                "    \"agentId\" : \"request_classifier\",\n" +
                "    \"config\" : {\n" +
                "      \"maxIterations\" : 1\n" +
                "    },\n" +
                "    \"description\" : \"将用户请求分类到特定类别\",\n" +
                "    \"inputKey\" : \"reception_output\",\n" +
                "    \"inputKeys\" : [ \"reception_output\" ],\n" +
                "    \"instructions\" : \"你是一个请求分类器。使用分类工具分析用户请求并将其分类为以下类别之一：'math', 'translation', 'data_analysis'。你必须只回复类别名称，不要其他内容。有效回复：'math', 'translation', 'data_analysis'。\",\n" +
                "    \"model\" : \"qwen-turbo\",\n" +
                "    \"name\" : \"请求分类Agent\",\n" +
                "    \"options\" : {\n" +
                "      \"controlType\" : \"auto\",\n" +
                "      \"disabled\" : false,\n" +
                "      \"locked\" : false,\n" +
                "      \"maxRetries\" : 3,\n" +
                "      \"outputVisibility\" : \"public\",\n" +
                "      \"ragK\" : 5,\n" +
                "      \"timeout\" : 30000,\n" +
                "      \"toggleAble\" : true\n" +
                "    },\n" +
                "    \"outputKey\" : \"request_category\",\n" +
                "    \"tools\" : [ {\n" +
                "      \"autoMock\" : true,\n" +
                "      \"name\" : \"classification_tool\"\n" +
                "    } ],\n" +
                "    \"type\" : \"react\"\n" +
                "  }, {\n" +
                "    \"agentId\" : \"math_agent\",\n" +
                "    \"config\" : {\n" +
                "      \"maxIterations\" : 5\n" +
                "    },\n" +
                "    \"description\" : \"处理数学问题\",\n" +
                "    \"inputKey\" : \"user_request\",\n" +
                "    \"inputKeys\" : [ \"user_request\", \"request_category\" ],\n" +
                "    \"instructions\" : \"你是一个数学专家。处理数学问题并提供解决方案。返回数学问题的结果。\",\n" +
                "    \"model\" : \"qwen-turbo\",\n" +
                "    \"name\" : \"数学Agent\",\n" +
                "    \"options\" : {\n" +
                "      \"controlType\" : \"auto\",\n" +
                "      \"disabled\" : false,\n" +
                "      \"locked\" : false,\n" +
                "      \"maxRetries\" : 3,\n" +
                "      \"outputVisibility\" : \"public\",\n" +
                "      \"ragK\" : 5,\n" +
                "      \"timeout\" : 30000,\n" +
                "      \"toggleAble\" : true\n" +
                "    },\n" +
                "    \"outputKey\" : \"math_solution\",\n" +
                "    \"tools\" : [ {\n" +
                "      \"autoMock\" : true,\n" +
                "      \"name\" : \"math_solver\"\n" +
                "    } ],\n" +
                "    \"type\" : \"react\"\n" +
                "  }, {\n" +
                "    \"agentId\" : \"translation_agent\",\n" +
                "    \"config\" : {\n" +
                "      \"maxIterations\" : 5\n" +
                "    },\n" +
                "    \"description\" : \"处理翻译问题\",\n" +
                "    \"inputKey\" : \"user_request\",\n" +
                "    \"inputKeys\" : [ \"user_request\", \"request_category\" ],\n" +
                "    \"instructions\" : \"你是一个翻译专家。处理翻译请求并提供翻译结果。返回翻译后的文本。\",\n" +
                "    \"model\" : \"qwen-turbo\",\n" +
                "    \"name\" : \"翻译Agent\",\n" +
                "    \"options\" : {\n" +
                "      \"controlType\" : \"auto\",\n" +
                "      \"disabled\" : false,\n" +
                "      \"locked\" : false,\n" +
                "      \"maxRetries\" : 3,\n" +
                "      \"outputVisibility\" : \"public\",\n" +
                "      \"ragK\" : 5,\n" +
                "      \"timeout\" : 30000,\n" +
                "      \"toggleAble\" : true\n" +
                "    },\n" +
                "    \"outputKey\" : \"translated_text\",\n" +
                "    \"tools\" : [ {\n" +
                "      \"autoMock\" : true,\n" +
                "      \"name\" : \"translator\"\n" +
                "    } ],\n" +
                "    \"type\" : \"react\"\n" +
                "  }, {\n" +
                "    \"agentId\" : \"data_analysis_agent\",\n" +
                "    \"config\" : {\n" +
                "      \"maxIterations\" : 5\n" +
                "    },\n" +
                "    \"description\" : \"处理数据分析问题\",\n" +
                "    \"inputKey\" : \"user_request\",\n" +
                "    \"inputKeys\" : [ \"user_request\", \"request_category\" ],\n" +
                "    \"instructions\" : \"你是一个数据分析专家。处理数据分析请求并提供分析结果。返回分析报告。\",\n" +
                "    \"model\" : \"qwen-turbo\",\n" +
                "    \"name\" : \"数据分析Agent\",\n" +
                "    \"options\" : {\n" +
                "      \"controlType\" : \"auto\",\n" +
                "      \"disabled\" : false,\n" +
                "      \"locked\" : false,\n" +
                "      \"maxRetries\" : 3,\n" +
                "      \"outputVisibility\" : \"public\",\n" +
                "      \"ragK\" : 5,\n" +
                "      \"timeout\" : 30000,\n" +
                "      \"toggleAble\" : true\n" +
                "    },\n" +
                "    \"outputKey\" : \"analysis_report\",\n" +
                "    \"tools\" : [ {\n" +
                "      \"autoMock\" : true,\n" +
                "      \"name\" : \"data_analyzer\"\n" +
                "    } ],\n" +
                "    \"type\" : \"react\"\n" +
                "  }, {\n" +
                "    \"agentId\" : \"report_generator\",\n" +
                "    \"config\" : {\n" +
                "      \"maxIterations\" : 1\n" +
                "    },\n" +
                "    \"description\" : \"生成最终报告\",\n" +
                "    \"inputKey\" : \"math_solution\",\n" +
                "    \"inputKeys\" : [ \"math_solution\", \"translated_text\", \"analysis_report\" ],\n" +
                "    \"instructions\" : \"你是一个报告生成专家。根据从其他Agent获取的数据生成最终报告。返回生成的报告。\",\n" +
                "    \"model\" : \"qwen-turbo\",\n" +
                "    \"name\" : \"报告生成Agent\",\n" +
                "    \"options\" : {\n" +
                "      \"controlType\" : \"auto\",\n" +
                "      \"disabled\" : false,\n" +
                "      \"locked\" : false,\n" +
                "      \"maxRetries\" : 3,\n" +
                "      \"outputVisibility\" : \"public\",\n" +
                "      \"ragK\" : 5,\n" +
                "      \"timeout\" : 30000,\n" +
                "      \"toggleAble\" : true\n" +
                "    },\n" +
                "    \"outputKey\" : \"final_report\",\n" +
                "    \"tools\" : [ ],\n" +
                "    \"type\" : \"llm\"\n" +
                "  } ],\n" +
                "  \"description\" : \"一个智能客服系统，包含问题分类、数学处理、翻译、数据分析和生成报告的Agent。\",\n" +
                "  \"edges\" : [ {\n" +
                "    \"config\" : { },\n" +
                "    \"edgeId\" : \"edge1\",\n" +
                "    \"edgeType\" : \"SEQUENTIAL\",\n" +
                "    \"fromAgentId\" : \"START\",\n" +
                "    \"label\" : \"开始\",\n" +
                "    \"toAgentId\" : \"reception_agent\"\n" +
                "  }, {\n" +
                "    \"config\" : { },\n" +
                "    \"edgeId\" : \"edge2\",\n" +
                "    \"edgeType\" : \"SEQUENTIAL\",\n" +
                "    \"fromAgentId\" : \"reception_agent\",\n" +
                "    \"label\" : \"分类请求\",\n" +
                "    \"toAgentId\" : \"request_classifier\"\n" +
                "  }, {\n" +
                "    \"condition\" : {\n" +
                "      \"request_category\" : \"math\"\n" +
                "    },\n" +
                "    \"config\" : { },\n" +
                "    \"edgeId\" : \"edge3\",\n" +
                "    \"edgeType\" : \"CONDITIONAL\",\n" +
                "    \"fromAgentId\" : \"request_classifier\",\n" +
                "    \"label\" : \"数学问题\",\n" +
                "    \"toAgentId\" : \"math_agent\"\n" +
                "  }, {\n" +
                "    \"condition\" : {\n" +
                "      \"request_category\" : \"translation\"\n" +
                "    },\n" +
                "    \"config\" : { },\n" +
                "    \"edgeId\" : \"edge4\",\n" +
                "    \"edgeType\" : \"CONDITIONAL\",\n" +
                "    \"fromAgentId\" : \"request_classifier\",\n" +
                "    \"label\" : \"翻译问题\",\n" +
                "    \"toAgentId\" : \"translation_agent\"\n" +
                "  }, {\n" +
                "    \"condition\" : {\n" +
                "      \"request_category\" : \"data_analysis\"\n" +
                "    },\n" +
                "    \"config\" : { },\n" +
                "    \"edgeId\" : \"edge5\",\n" +
                "    \"edgeType\" : \"CONDITIONAL\",\n" +
                "    \"fromAgentId\" : \"request_classifier\",\n" +
                "    \"label\" : \"数据分析问题\",\n" +
                "    \"toAgentId\" : \"data_analysis_agent\"\n" +
                "  }, {\n" +
                "    \"config\" : { },\n" +
                "    \"edgeId\" : \"edge6\",\n" +
                "    \"edgeType\" : \"SEQUENTIAL\",\n" +
                "    \"fromAgentId\" : \"math_agent\",\n" +
                "    \"label\" : \"生成报告\",\n" +
                "    \"toAgentId\" : \"report_generator\"\n" +
                "  }, {\n" +
                "    \"config\" : { },\n" +
                "    \"edgeId\" : \"edge7\",\n" +
                "    \"edgeType\" : \"SEQUENTIAL\",\n" +
                "    \"fromAgentId\" : \"translation_agent\",\n" +
                "    \"label\" : \"生成报告\",\n" +
                "    \"toAgentId\" : \"report_generator\"\n" +
                "  }, {\n" +
                "    \"config\" : { },\n" +
                "    \"edgeId\" : \"edge8\",\n" +
                "    \"edgeType\" : \"SEQUENTIAL\",\n" +
                "    \"fromAgentId\" : \"data_analysis_agent\",\n" +
                "    \"label\" : \"生成报告\",\n" +
                "    \"toAgentId\" : \"report_generator\"\n" +
                "  }, {\n" +
                "    \"config\" : { },\n" +
                "    \"edgeId\" : \"edge9\",\n" +
                "    \"edgeType\" : \"SEQUENTIAL\",\n" +
                "    \"fromAgentId\" : \"report_generator\",\n" +
                "    \"label\" : \"结束\",\n" +
                "    \"toAgentId\" : \"END\"\n" +
                "  } ],\n" +
                "  \"globalConfig\" : {\n" +
                "    \"maxRetries\" : 3,\n" +
                "    \"timeout\" : 60000,\n" +
                "    \"enableLogging\" : true,\n" +
                "    \"enableMetrics\" : true,\n" +
                "    \"maxLoopIterations\" : 5,\n" +
                "    \"parallelExecution\" : false\n" +
                "  },\n" +
                "  \"metadata\" : {\n" +
                "    \"author\" : \"AI Assistant\",\n" +
                "    \"category\" : \"demonstration\",\n" +
                "    \"createdAt\" : \"2024-01-01T00:00:00Z\",\n" +
                "    \"customFields\" : {\n" +
                "      \"estimatedDuration\" : \"10-20 minutes\",\n" +
                "      \"complexity\" : \"medium\",\n" +
                "      \"requiredAgents\" : 6,\n" +
                "      \"agentTypes\" : [ \"llm\", \"react\" ],\n" +
                "      \"features\" : [ \"Output Constraints\", \"Conditional Routing\", \"Structured Output\" ]\n" +
                "    },\n" +
                "    \"lastUpdatedAt\" : \"2024-01-01T00:00:00Z\",\n" +
                "    \"tags\" : [ \"customer-service\", \"multi-agent-system\", \"workflow\" ]\n" +
                "  },\n" +
                "  \"name\" : \"智能客服系统\",\n" +
                "  \"version\" : \"1.0.0\",\n" +
                "  \"workflowId\" : \"smart_customer_service_system\"\n" +
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