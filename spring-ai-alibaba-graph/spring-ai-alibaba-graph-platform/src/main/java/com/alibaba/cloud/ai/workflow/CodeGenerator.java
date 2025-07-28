//package com.alibaba.cloud.ai.workflow;
//
//import com.alibaba.cloud.ai.graph.*;
//import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
//import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
//import com.alibaba.cloud.ai.graph.action.NodeAction;
//import com.alibaba.cloud.ai.graph.exception.GraphStateException;
//import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.model.ChatModel;
//import org.springframework.util.StringUtils;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//import static com.alibaba.cloud.ai.graph.StateGraph.END;
//import static com.alibaba.cloud.ai.graph.StateGraph.START;
//import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
//import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
//
///**
// * 代码生成器，将工作流配置转换为基于 NodeAction 的 Java 代码
// *
// * @author AI Assistant
// */
//public class CodeGenerator {
//
//    /**
//     * 生成工作流控制器代码
//     */
//    public String generateControllerCode(WorkflowSchema schema) {
//        StringBuilder code = new StringBuilder();
//
//        // 包声明
//        code.append("package com.alibaba.cloud.ai.example.generated;\n\n");
//
//        // 导入语句
//        code.append("import com.alibaba.cloud.ai.graph.*;\n");
//        code.append("import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;\n");
//        code.append("import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;\n");
//        code.append("import com.alibaba.cloud.ai.graph.action.NodeAction;\n");
//        code.append("import com.alibaba.cloud.ai.graph.exception.GraphStateException;\n");
//        code.append("import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;\n");
//        code.append("import org.springframework.ai.chat.client.ChatClient;\n");
//        code.append("import org.springframework.ai.chat.model.ChatModel;\n");
//        code.append("import org.springframework.web.bind.annotation.*;\n");
//        code.append("import java.util.Map;\n");
//        code.append("import java.util.HashMap;\n");
//        code.append("import java.util.List;\n\n");
//
//        // 静态导入
//        code.append("import static com.alibaba.cloud.ai.graph.StateGraph.END;\n");
//        code.append("import static com.alibaba.cloud.ai.graph.StateGraph.START;\n");
//        code.append("import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;\n");
//        code.append("import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;\n\n");
//
//        // 类声明
//        String className = toCamelCase(schema.getName()) + "Controller";
//        code.append("@RestController\n");
//        code.append("@RequestMapping(\"/generated/").append(schema.getWorkflowId()).append("\")\n");
//        code.append("public class ").append(className).append(" {\n\n");
//
//        // 字段
//        code.append("    private final ChatClient chatClient;\n");
//        code.append("    private final CompiledGraph compiledGraph;\n\n");
//
//        // 构造函数
//        code.append("    public ").append(className).append("(ChatModel chatModel) throws GraphStateException {\n");
//        code.append("        this.chatClient = ChatClient.builder(chatModel).build();\n");
//        code.append("        this.compiledGraph = initGraph();\n");
//        code.append("    }\n\n");
//
//        // initGraph 方法
//        code.append("    private CompiledGraph initGraph() throws GraphStateException {\n");
//        code.append("        OverAllStateFactory stateFactory = () -> {\n");
//        code.append("            OverAllState state = new OverAllState();\n");
//
//        // 注册键和策略
//        for (WorkflowSchema.NodeConfig node : schema.getNodes()) {
//            if (node.getInputMapping() != null) {
//                for (Object value : node.getInputMapping().values()) {
//                    code.append("            state.registerKeyAndStrategy(\"").append(value).append("\", new ReplaceStrategy());\n");
//                }
//            }
//            if (node.getOutputMapping() != null) {
//                for (Object value : node.getOutputMapping().values()) {
//                    code.append("            state.registerKeyAndStrategy(\"").append(value).append("\", new ReplaceStrategy());\n");
//                }
//            }
//        }
//
//        code.append("            return state;\n");
//        code.append("        };\n\n");
//
//        // 创建图形
//        code.append("        StateGraph graph = new StateGraph(stateFactory);\n\n");
//
//        // 添加节点
//        for (WorkflowSchema.NodeConfig node : schema.getNodes()) {
//            code.append("        // 添加节点: ").append(node.getName()).append("\n");
//            code.append("        graph.addNode(\"").append(node.getNodeId()).append("\", node_async(create").append(toCamelCase(node.getName())).append("Node()));\n");
//        }
//        code.append("\n");
//
//        // 添加边
//        for (WorkflowSchema.EdgeConfig edge : schema.getEdges()) {
//            if (START.equals(edge.getFromNodeId())) {
//                code.append("        graph.addEdge(START, \"").append(edge.getToNodeId()).append("\");\n");
//            } else if (END.equals(edge.getToNodeId())) {
//                if (edge.getCondition() != null) {
//                    code.append("        graph.addConditionalEdges(\"").append(edge.getFromNodeId()).append("\", createConditionEdge(\"").append(edge.getCondition()).append("\"), Map.of(\"true\", END, \"false\", END));\n");
//                } else {
//                    code.append("        graph.addEdge(\"").append(edge.getFromNodeId()).append("\", END);\n");
//                }
//            } else {
//                if (edge.getCondition() != null) {
//                    code.append("        graph.addConditionalEdges(\"").append(edge.getFromNodeId()).append("\", createConditionEdge(\"").append(edge.getCondition()).append("\"), Map.of(\"true\", \"").append(edge.getToNodeId()).append("\", \"false\", \"").append(edge.getToNodeId()).append("\"));\n");
//                } else {
//                    code.append("        graph.addEdge(\"").append(edge.getFromNodeId()).append("\", \"").append(edge.getToNodeId()).append("\");\n");
//                }
//            }
//        }
//
//        code.append("\n");
//        code.append("        return graph.compile();\n");
//        code.append("    }\n\n");
//
//        // 生成节点创建方法
//        for (WorkflowSchema.NodeConfig node : schema.getNodes()) {
//            code.append(generateNodeMethod(node));
//        }
//
//        // 生成条件边方法
//        code.append("    private AsyncEdgeAction createConditionEdge(String condition) {\n");
//        code.append("        return edge_async(state -> {\n");
//        code.append("            if (condition.contains(\"==\")) {\n");
//        code.append("                String[] parts = condition.split(\"==\");\n");
//        code.append("                String key = parts[0].trim();\n");
//        code.append("                String value = parts[1].trim().replace(\"\\\"\", \"\");\n");
//        code.append("                return state.value(key).map(val -> val.toString().equals(value)).orElse(false) ? \"true\" : \"false\";\n");
//        code.append("            } else if (condition.contains(\"!=\")) {\n");
//        code.append("                String[] parts = condition.split(\"!=\");\n");
//        code.append("                String key = parts[0].trim();\n");
//        code.append("                String value = parts[1].trim().replace(\"\\\"\", \"\");\n");
//        code.append("                return state.value(key).map(val -> !val.toString().equals(value)).orElse(false) ? \"true\" : \"false\";\n");
//        code.append("            } else if (condition.contains(\">\")) {\n");
//        code.append("                String[] parts = condition.split(\">\");\n");
//        code.append("                String key = parts[0].trim();\n");
//        code.append("                String value = parts[1].trim();\n");
//        code.append("                return state.value(key).map(val -> {\n");
//        code.append("                    try {\n");
//        code.append("                        double valNum = Double.parseDouble(val.toString());\n");
//        code.append("                        double compareNum = Double.parseDouble(value);\n");
//        code.append("                        return valNum > compareNum;\n");
//        code.append("                    } catch (NumberFormatException e) {\n");
//        code.append("                        return false;\n");
//        code.append("                    }\n");
//        code.append("                }).orElse(false) ? \"true\" : \"false\";\n");
//        code.append("            }\n");
//        code.append("            return \"true\";\n");
//        code.append("        });\n");
//        code.append("    }\n\n");
//
//        // 生成API方法
//        code.append("    @PostMapping(\"/execute\")\n");
//        code.append("    public Map<String, Object> execute(@RequestBody Map<String, Object> input) {\n");
//        code.append("        return compiledGraph.invoke(input).get().data();\n");
//        code.append("    }\n\n");
//
//        code.append("    @GetMapping(\"/health\")\n");
//        code.append("    public Map<String, Object> health() {\n");
//        code.append("        Map<String, Object> response = new HashMap<>();\n");
//        code.append("        response.put(\"status\", \"UP\");\n");
//        code.append("        response.put(\"workflow\", \"").append(schema.getName()).append("\");\n");
//        code.append("        response.put(\"workflowId\", \"").append(schema.getWorkflowId()).append("\");\n");
//        code.append("        return response;\n");
//        code.append("    }\n");
//
//        code.append("}\n");
//
//        return code.toString();
//    }
//
//    /**
//     * 生成节点方法
//     */
//    private String generateNodeMethod(WorkflowSchema.NodeConfig node) {
//        StringBuilder method = new StringBuilder();
//
//        String methodName = "create" + toCamelCase(node.getName()) + "Node";
//        method.append("    private NodeAction ").append(methodName).append("() {\n");
//
//        switch (node.getNodeType().toLowerCase()) {
//            case "llm":
//                method.append(generateLlmNodeMethod(node));
//                break;
//            case "simple":
//                method.append(generateSimpleNodeMethod(node));
//                break;
//            case "custom":
//                method.append(generateCustomNodeMethod(node));
//                break;
//            case "condition":
//                method.append(generateConditionNodeMethod(node));
//                break;
//            default:
//                method.append("        return state -> new HashMap<>();\n");
//        }
//
//        method.append("    }\n\n");
//        return method.toString();
//    }
//
//    /**
//     * 生成LLM节点方法
//     */
//    private String generateLlmNodeMethod(WorkflowSchema.NodeConfig node) {
//        Map<String, Object> config = node.getConfig();
//        String systemPrompt = (String) config.get("systemPrompt");
//        String userPrompt = (String) config.get("userPrompt");
//        String outputKey = (String) config.getOrDefault("outputKey", "llm_response");
//
//        StringBuilder method = new StringBuilder();
//        method.append("        return state -> {\n");
//        method.append("            try {\n");
//        method.append("                String input = state.value(\"input\", String.class).orElse(\"\");\n");
//        method.append("                String prompt = \"").append(userPrompt != null ? userPrompt : "").append("\".replace(\"{input}\", input);\n");
//        method.append("                \n");
//        method.append("                // 这里应该调用实际的LLM服务\n");
//        method.append("                String response = \"LLM Response for: \" + prompt;\n");
//        method.append("                \n");
//        method.append("                Map<String, Object> result = new HashMap<>();\n");
//        method.append("                result.put(\"").append(outputKey).append("\", response);\n");
//        method.append("                return result;\n");
//        method.append("            } catch (Exception e) {\n");
//        method.append("                throw new RuntimeException(e);\n");
//        method.append("            }\n");
//        method.append("        };\n");
//
//        return method.toString();
//    }
//
//    /**
//     * 生成简单节点方法
//     */
//    private String generateSimpleNodeMethod(WorkflowSchema.NodeConfig node) {
//        Map<String, Object> config = node.getConfig();
//        String inputKey = (String) config.getOrDefault("inputKey", "input");
//        String outputKey = (String) config.getOrDefault("outputKey", "output");
//
//        StringBuilder method = new StringBuilder();
//        method.append("        return state -> {\n");
//        method.append("            String input = state.value(\"").append(inputKey).append("\", String.class).orElse(\"默认输入\");\n");
//        method.append("            String result = \"节点 ").append(node.getName()).append(" 处理结果: \" + input.toUpperCase();\n");
//        method.append("            \n");
//        method.append("            Map<String, Object> outputs = new HashMap<>();\n");
//        method.append("            outputs.put(\"").append(outputKey).append("\", result);\n");
//        method.append("            outputs.put(\"").append(node.getNodeId()).append("_processed\", true);\n");
//        method.append("            return outputs;\n");
//        method.append("        };\n");
//
//        return method.toString();
//    }
//
//    /**
//     * 生成自定义节点方法
//     */
//    private String generateCustomNodeMethod(WorkflowSchema.NodeConfig node) {
//        Map<String, Object> config = node.getConfig();
//        String customLogic = (String) config.get("customLogic");
//
//        StringBuilder method = new StringBuilder();
//        method.append("        return state -> {\n");
//        method.append("            Map<String, Object> inputs = new HashMap<>();\n");
//
//        if (node.getInputMapping() != null) {
//            for (Map.Entry<String, Object> entry : node.getInputMapping().entrySet()) {
//                method.append("            state.value(\"").append(entry.getValue()).append("\").ifPresent(value -> inputs.put(\"").append(entry.getKey()).append("\", value));\n");
//            }
//        }
//
//        method.append("            \n");
//        method.append("            // 执行自定义逻辑: ").append(customLogic).append("\n");
//        method.append("            Map<String, Object> result = new HashMap<>();\n");
//
//        if ("uppercase".equals(customLogic)) {
//            method.append("            String input = (String) inputs.get(\"input\");\n");
//            method.append("            if (input != null) {\n");
//            method.append("                result.put(\"output\", input.toUpperCase());\n");
//            method.append("            }\n");
//        } else if ("lowercase".equals(customLogic)) {
//            method.append("            String input = (String) inputs.get(\"input\");\n");
//            method.append("            if (input != null) {\n");
//            method.append("                result.put(\"output\", input.toLowerCase());\n");
//            method.append("            }\n");
//        } else {
//            method.append("            result.put(\"output\", \"Processed: \" + inputs);\n");
//        }
//
//        method.append("            \n");
//        method.append("            // 处理输出映射\n");
//        method.append("            Map<String, Object> outputs = new HashMap<>();\n");
//        if (node.getOutputMapping() != null) {
//            for (Map.Entry<String, Object> entry : node.getOutputMapping().entrySet()) {
//                method.append("            if (result.containsKey(\"").append(entry.getKey()).append("\")) {\n");
//                method.append("                outputs.put(\"").append(entry.getValue()).append("\", result.get(\"").append(entry.getKey()).append("\"));\n");
//                method.append("            }\n");
//            }
//        }
//        method.append("            return outputs;\n");
//        method.append("        };\n");
//
//        return method.toString();
//    }
//
//    /**
//     * 生成条件节点方法
//     */
//    private String generateConditionNodeMethod(WorkflowSchema.NodeConfig node) {
//        Map<String, Object> config = node.getConfig();
//        String condition = (String) config.get("condition");
//
//        StringBuilder method = new StringBuilder();
//        method.append("        return state -> {\n");
//        method.append("            String conditionExpression = \"").append(condition != null ? condition : "").append("\";\n");
//        method.append("            boolean conditionResult = false;\n");
//        method.append("            \n");
//        method.append("            if (conditionExpression.contains(\"==\")) {\n");
//        method.append("                String[] parts = conditionExpression.split(\"==\");\n");
//        method.append("                String key = parts[0].trim();\n");
//        method.append("                String value = parts[1].trim().replace(\"\\\"\", \"\");\n");
//        method.append("                conditionResult = state.value(key).map(val -> val.toString().equals(value)).orElse(false);\n");
//        method.append("            } else if (conditionExpression.contains(\"!=\")) {\n");
//        method.append("                String[] parts = conditionExpression.split(\"!=\");\n");
//        method.append("                String key = parts[0].trim();\n");
//        method.append("                String value = parts[1].trim().replace(\"\\\"\", \"\");\n");
//        method.append("                conditionResult = state.value(key).map(val -> !val.toString().equals(value)).orElse(false);\n");
//        method.append("            }\n");
//        method.append("            \n");
//        method.append("            Map<String, Object> result = new HashMap<>();\n");
//        method.append("            result.put(\"condition_result\", conditionResult);\n");
//        method.append("            result.put(\"next_node\", conditionResult ? \"").append(config.get("trueNext")).append("\" : \"").append(config.get("falseNext")).append("\");\n");
//        method.append("            return result;\n");
//        method.append("        };\n");
//
//        return method.toString();
//    }
//
//    /**
//     * 转换为驼峰命名
//     */
//    private String toCamelCase(String str) {
//        if (!StringUtils.hasText(str)) {
//            return "Default";
//        }
//
//        String[] words = str.split("[\\s_-]+");
//        StringBuilder result = new StringBuilder();
//
//        for (String word : words) {
//            if (word.length() > 0) {
//                result.append(Character.toUpperCase(word.charAt(0)))
//                      .append(word.substring(1).toLowerCase());
//            }
//        }
//
//        return result.toString();
//    }
//}