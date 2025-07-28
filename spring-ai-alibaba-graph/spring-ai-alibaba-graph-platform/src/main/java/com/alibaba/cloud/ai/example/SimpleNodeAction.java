//package com.alibaba.cloud.ai.example;
//
//import com.alibaba.cloud.ai.graph.OverAllState;
//import com.alibaba.cloud.ai.graph.action.NodeAction;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * 简单的 NodeAction 实现示例
// *
// * @author AI Assistant
// */
//public class SimpleNodeAction implements NodeAction {
//
//    private static final Logger logger = LoggerFactory.getLogger(SimpleNodeAction.class);
//
//    private final String nodeName;
//    private final String inputKey;
//    private final String outputKey;
//
//    public SimpleNodeAction(String nodeName, String inputKey, String outputKey) {
//        this.nodeName = nodeName;
//        this.inputKey = inputKey;
//        this.outputKey = outputKey;
//    }
//
//    @Override
//    public Map<String, Object> apply(OverAllState state) throws Exception {
//        logger.info("执行节点: {}", nodeName);
//
//        // 从状态中获取输入数据
//        String input = state.value(inputKey, String.class)
//                .orElse("默认输入");
//
//        logger.info("节点 {} 接收到输入: {}", nodeName, input);
//
//        // 执行节点逻辑
//        String result = processInput(input);
//
//        // 更新状态
//        Map<String, Object> updatedState = new HashMap<>();
//        updatedState.put(outputKey, result);
//        updatedState.put(nodeName + "_processed", true);
//        updatedState.put(nodeName + "_timestamp", System.currentTimeMillis());
//
//        logger.info("节点 {} 处理完成，输出: {}", nodeName, result);
//
//        return updatedState;
//    }
//
//    /**
//     * 处理输入数据的逻辑
//     */
//    private String processInput(String input) {
//        // 这里可以添加具体的业务逻辑
//        return "节点 " + nodeName + " 处理结果: " + input.toUpperCase();
//    }
//
//    /**
//     * 创建简单的文本处理节点
//     */
//    public static SimpleNodeAction createTextProcessor(String nodeName) {
//        return new SimpleNodeAction(nodeName, "input", "output");
//    }
//
//    /**
//     * 创建数据转换节点
//     */
//    public static SimpleNodeAction createDataTransformer(String nodeName, String inputKey, String outputKey) {
//        return new SimpleNodeAction(nodeName, inputKey, outputKey);
//    }
//}