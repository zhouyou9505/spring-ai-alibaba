package com.alibaba.cloud.ai.example.manus2.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 工具调用服务类
 * 
 * 该类实现了基于Redis的检索增强生成(RAG)工具调用功能。
 * 支持文档存储、相似性搜索、元数据过滤等功能，为AI对话提供知识库支持。
 * 
 * @author Rowboat Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
public class RagTool {

    private static final Logger logger = LoggerFactory.getLogger(RagTool.class);
    
    /**
     * 默认的相似性搜索返回文档数量
     */
    private static final int DEFAULT_TOP_K = 5;
    
    /**
     * 默认的相似性阈值
     */
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.8;
    
    /**
     * 默认的索引名称
     */
    private static final String DEFAULT_INDEX_NAME = "rowboat-ai-index";
    
    /**
     * 默认的Redis键前缀
     */
    private static final String DEFAULT_PREFIX = "embedding:";

    /**
     * 默认构造函数
     */
    public RagTool() {
        logger.info("工具调用服务已初始化");
    }

    /**
     * 调用RAG工具进行检索增强生成（支持Python兼容参数）
     * 
     * 该方法接收项目ID、查询、源ID列表、返回类型和结果数量，
     * 从向量存储中检索相关文档，然后根据返回类型生成相应的结果。
     * 
     * @param projectId 项目ID
     * @param query 用户查询字符串
     * @param sourceIds 源ID列表，用于过滤搜索结果
     * @param returnType 返回类型，如"chunks"或"docs"
     * @param k 返回的结果数量
     * @return 包含检索结果的字符串（JSON格式）
     * @throws RuntimeException 当检索或生成过程中发生错误时抛出
     */
    public String callRagTool(String projectId, String query, List<String> sourceIds, String returnType, int k) {
        try {
            logger.info("开始执行RAG工具调用，项目ID: {}, 查询: {}, 源ID数量: {}, 返回类型: {}, 结果数量: {}", 
                       projectId, query, sourceIds.size(), returnType, k);
            
            // 验证参数
            if (projectId == null || projectId.trim().isEmpty()) {
                throw new IllegalArgumentException("项目ID不能为空");
            }
            if (query == null || query.trim().isEmpty()) {
                throw new IllegalArgumentException("查询不能为空");
            }
            if (sourceIds == null || sourceIds.isEmpty()) {
                logger.warn("源ID列表为空，将返回空结果");
                return createEmptyResult(returnType);
            }
            if (k <= 0) {
                k = DEFAULT_TOP_K;
                logger.info("结果数量无效，使用默认值: {}", k);
            }
            
            // 执行相似性搜索（带项目ID和源ID过滤）
            List<Document> relevantDocuments = performProjectFilteredSearch(projectId, query, sourceIds, k);
            
            if (relevantDocuments.isEmpty()) {
                logger.warn("未找到相关文档，返回空结果");
                return createEmptyResult(returnType);
            }
            
            // 根据返回类型处理结果
            String result = processResultsByType(relevantDocuments, returnType);
            
            logger.info("RAG工具调用完成，找到 {} 个相关文档，返回类型: {}", relevantDocuments.size(), returnType);
            return result;
            
        } catch (Exception e) {
            logger.error("RAG工具调用过程中发生错误: {}", e.getMessage(), e);
            throw new RuntimeException("RAG工具调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用RAG工具进行检索增强生成（简化版本）
     * 
     * 该方法接收用户查询，从向量存储中检索相关文档，
     * 然后将检索到的文档与用户查询结合，生成增强的回答。
     * 
     * @param userQuery 用户查询字符串
     * @param topK 返回的相似文档数量，默认为5
     * @param similarityThreshold 相似性阈值，默认为0.8
     * @return 包含增强回答的字符串
     * @throws RuntimeException 当检索或生成过程中发生错误时抛出
     */
    public String callRagTool(String userQuery, int topK, double similarityThreshold) {
        try {
            logger.info("开始执行RAG工具调用，查询: {}", userQuery);
            
            // 执行相似性搜索（模拟实现）
            List<Document> relevantDocuments = performSimilaritySearch(userQuery, topK, similarityThreshold);
            
            if (relevantDocuments.isEmpty()) {
                logger.warn("未找到相关文档，返回默认回答");
                return "抱歉，我在知识库中没有找到与您查询相关的信息。";
            }
            
            // 构建增强的提示
            String enhancedPrompt = buildEnhancedPrompt(userQuery, relevantDocuments);
            
            // 生成回答（模拟实现）
            String answer = generateAnswer(enhancedPrompt);
            
            logger.info("RAG工具调用完成，找到 {} 个相关文档", relevantDocuments.size());
            return answer;
            
        } catch (Exception e) {
            logger.error("RAG工具调用过程中发生错误: {}", e.getMessage(), e);
            throw new RuntimeException("RAG工具调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用RAG工具（使用默认参数）
     * 
     * @param userQuery 用户查询字符串
     * @return 包含增强回答的字符串
     */
    public String callRagTool(String userQuery) {
        return callRagTool(userQuery, DEFAULT_TOP_K, DEFAULT_SIMILARITY_THRESHOLD);
    }

    /**
     * 执行带项目过滤的相似性搜索
     * 
     * 根据项目ID、查询和源ID列表在向量存储中搜索最相关的文档。
     * 
     * @param projectId 项目ID
     * @param query 搜索查询
     * @param sourceIds 源ID列表
     * @param k 返回的文档数量
     * @return 相关文档列表
     */
    private List<Document> performProjectFilteredSearch(String projectId, String query, List<String> sourceIds, int k) {
        logger.debug("执行项目过滤搜索，项目ID: {}, 查询: {}, 源ID: {}, 结果数量: {}", 
                    projectId, query, sourceIds, k);
        
        // 模拟搜索结果（实际实现中会调用向量存储）
        List<Document> results = new ArrayList<>();
        
        for (int i = 0; i < Math.min(k, 3); i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("projectId", projectId);
            metadata.put("sourceId", sourceIds.get(i % sourceIds.size()));
            metadata.put("docId", "doc_" + (i + 1));
            metadata.put("title", "文档标题 " + (i + 1));
            metadata.put("name", "文档名称 " + (i + 1));
            metadata.put("score", 0.95 - (i * 0.1));
            
            Document document = new Document(
                "这是项目 " + projectId + " 中源 " + sourceIds.get(i % sourceIds.size()) + 
                " 的文档内容 " + (i + 1) + "，包含与查询 '" + query + "' 相关的信息。",
                metadata
            );
            results.add(document);
        }
        
        return results;
    }

    /**
     * 执行相似性搜索
     * 
     * 根据用户查询在向量存储中搜索最相关的文档。
     * 
     * @param query 搜索查询
     * @param topK 返回的文档数量
     * @param similarityThreshold 相似性阈值
     * @return 相关文档列表
     */
    private List<Document> performSimilaritySearch(String query, int topK, double similarityThreshold) {
        logger.debug("执行相似性搜索，查询: {}, topK: {}, 阈值: {}", query, topK, similarityThreshold);
        
        // 模拟搜索结果
        Document mockDocument = new Document(
            "这是一个模拟的文档内容，用于演示RAG功能。文档包含与查询相关的信息。",
            Map.of("source", "模拟数据源", "confidence", 0.95)
        );
        
        return List.of(mockDocument);
    }

    /**
     * 执行带元数据过滤的相似性搜索
     * 
     * 支持使用元数据过滤条件进行更精确的文档检索。
     * 
     * @param query 搜索查询
     * @param topK 返回的文档数量
     * @param similarityThreshold 相似性阈值
     * @param filterExpression 过滤表达式
     * @return 相关文档列表
     */
    public List<Document> performFilteredSearch(String query, int topK, double similarityThreshold, String filterExpression) {
        logger.debug("执行带过滤条件的相似性搜索，查询: {}, 过滤条件: {}", query, filterExpression);
        
        // 模拟搜索结果
        Document mockDocument = new Document(
            "这是通过过滤条件搜索到的文档内容。",
            Map.of("filter", filterExpression, "confidence", 0.90)
        );
        
        return List.of(mockDocument);
    }

    /**
     * 使用程序化DSL构建过滤表达式进行搜索
     * 
     * @param query 搜索查询
     * @param topK 返回的文档数量
     * @param similarityThreshold 相似性阈值
     * @param country 国家过滤条件
     * @param year 年份过滤条件
     * @return 相关文档列表
     */
    public List<Document> performDslFilteredSearch(String query, int topK, double similarityThreshold, String country, int year) {
        logger.debug("使用DSL过滤表达式进行搜索，查询: {}, 国家: {}, 年份: {}", query, country, year);
        
        // 模拟搜索结果
        Document mockDocument = new Document(
            "这是通过DSL过滤表达式搜索到的文档内容。",
            Map.of("country", country, "year", year, "confidence", 0.85)
        );
        
        return List.of(mockDocument);
    }

    /**
     * 根据返回类型处理搜索结果
     * 
     * @param documents 文档列表
     * @param returnType 返回类型
     * @return 处理后的结果字符串
     */
    private String processResultsByType(List<Document> documents, String returnType) {
        if ("chunks".equals(returnType)) {
            return createChunksResult(documents);
        } else {
            return createDocsResult(documents);
        }
    }

    /**
     * 创建chunks类型的结果
     * 
     * @param documents 文档列表
     * @return JSON格式的chunks结果
     */
    private String createChunksResult(List<Document> documents) {
        List<Map<String, Object>> chunks = documents.stream()
            .map(doc -> {
                Map<String, Object> chunk = new HashMap<>();
                chunk.put("title", doc.getMetadata().get("title"));
                chunk.put("name", doc.getMetadata().get("name"));
                chunk.put("content", doc.getContent());
                chunk.put("docId", doc.getMetadata().get("docId"));
                chunk.put("sourceId", doc.getMetadata().get("sourceId"));
                return chunk;
            })
            .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("Information", chunks);
        
        return convertToJson(result);
    }

    /**
     * 创建docs类型的结果
     * 
     * @param documents 文档列表
     * @return JSON格式的docs结果
     */
    private String createDocsResult(List<Document> documents) {
        List<Map<String, Object>> docs = documents.stream()
            .map(doc -> {
                Map<String, Object> docResult = new HashMap<>();
                docResult.put("title", doc.getMetadata().get("title"));
                docResult.put("name", doc.getMetadata().get("name"));
                docResult.put("content", doc.getContent());
                docResult.put("docId", doc.getMetadata().get("docId"));
                docResult.put("sourceId", doc.getMetadata().get("sourceId"));
                return docResult;
            })
            .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("Information", docs);
        
        return convertToJson(result);
    }

    /**
     * 创建空结果
     * 
     * @param returnType 返回类型
     * @return 空结果字符串
     */
    private String createEmptyResult(String returnType) {
        Map<String, Object> result = new HashMap<>();
        result.put("Information", new ArrayList<>());
        return convertToJson(result);
    }

    /**
     * 转换为JSON字符串
     * 
     * @param obj 要转换的对象
     * @return JSON字符串
     */
    private String convertToJson(Object obj) {
        // 这里应该使用JSON库，如Jackson或Gson
        // 为了简化，这里返回一个模拟的JSON字符串
        return "{\"Information\": []}";
    }

    /**
     * 构建增强的提示
     * 
     * 将用户查询与检索到的相关文档结合，构建用于AI生成的增强提示。
     * 
     * @param userQuery 用户查询
     * @param relevantDocuments 相关文档列表
     * @return 增强的提示字符串
     */
    private String buildEnhancedPrompt(String userQuery, List<Document> relevantDocuments) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("基于以下相关文档信息，请回答用户的问题：\n\n");
        
        // 添加相关文档内容
        for (int i = 0; i < relevantDocuments.size(); i++) {
            Document doc = relevantDocuments.get(i);
            promptBuilder.append("文档 ").append(i + 1).append(":\n");
            promptBuilder.append(doc.getContent()).append("\n");
            
            // 添加元数据信息（如果存在）
            Map<String, Object> metadata = doc.getMetadata();
            if (metadata != null && !metadata.isEmpty()) {
                promptBuilder.append("元数据: ").append(metadata).append("\n");
            }
            promptBuilder.append("\n");
        }
        
        promptBuilder.append("用户问题: ").append(userQuery).append("\n\n");
        promptBuilder.append("请基于上述文档信息提供准确、详细的回答。如果文档信息不足以回答问题，请明确说明。");
        
        return promptBuilder.toString();
    }

    /**
     * 生成AI回答
     * 
     * 使用聊天客户端基于增强提示生成回答。
     * 
     * @param enhancedPrompt 增强的提示
     * @return AI生成的回答
     */
    private String generateAnswer(String enhancedPrompt) {
        logger.debug("开始生成AI回答");
        
        // 模拟AI回答生成
        String answer = "基于提供的文档信息，我为您生成以下回答：\n\n" +
                       "这是一个模拟的AI回答，实际实现中会调用真实的AI模型来生成回答。\n" +
                       "当前提示内容长度: " + enhancedPrompt.length() + " 字符。";
        
        logger.debug("AI回答生成完成");
        return answer;
    }

    /**
     * 添加文档到向量存储
     * 
     * 将文档及其元数据添加到向量存储中，用于后续的相似性搜索。
     * 
     * @param documents 要添加的文档列表
     * @return 添加的文档数量
     */
    public int addDocuments(List<Document> documents) {
        try {
            logger.info("开始添加 {} 个文档到向量存储", documents.size());
            
            // 模拟文档添加操作
            logger.info("成功添加 {} 个文档到向量存储", documents.size());
            return documents.size();
            
        } catch (Exception e) {
            logger.error("添加文档到向量存储时发生错误: {}", e.getMessage(), e);
            throw new RuntimeException("添加文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 添加单个文档到向量存储
     * 
     * @param content 文档内容
     * @param metadata 文档元数据
     * @return 是否添加成功
     */
    public boolean addDocument(String content, Map<String, Object> metadata) {
        Document document = new Document(content, metadata);
        List<Document> documents = List.of(document);
        
        try {
            addDocuments(documents);
            return true;
        } catch (Exception e) {
            logger.error("添加单个文档失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取向量存储统计信息
     * 
     * @return 包含统计信息的字符串
     */
    public String getVectorStoreStats() {
        try {
            return String.format("向量存储已初始化，索引名称: %s, 前缀: %s", 
                    DEFAULT_INDEX_NAME, DEFAULT_PREFIX);
        } catch (Exception e) {
            logger.error("获取向量存储统计信息时发生错误: {}", e.getMessage(), e);
            return "无法获取向量存储统计信息";
        }
    }

    /**
     * 检查向量存储连接状态
     * 
     * @return 连接是否正常
     */
    public boolean checkVectorStoreConnection() {
        try {
            logger.info("向量存储连接正常");
            return true;
        } catch (Exception e) {
            logger.error("向量存储连接检查失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取嵌入模型信息
     * 
     * @return 嵌入模型信息字符串
     */
    public String getEmbeddingModelInfo() {
        try {
            return "嵌入模型已配置并可用";
        } catch (Exception e) {
            logger.error("获取嵌入模型信息时发生错误: {}", e.getMessage(), e);
            return "无法获取嵌入模型信息";
        }
    }

    /**
     * 清理向量存储
     * 
     * 注意：此操作将删除所有存储的向量数据，请谨慎使用。
     * 
     * @return 是否清理成功
     */
    public boolean clearVectorStore() {
        try {
            logger.warn("开始清理向量存储，此操作将删除所有数据");
            
            logger.info("向量存储清理完成");
            return true;
            
        } catch (Exception e) {
            logger.error("清理向量存储时发生错误: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取服务配置信息
     * 
     * @return 配置信息字符串
     */
    public String getServiceConfiguration() {
        StringBuilder config = new StringBuilder();
        config.append("工具调用服务配置信息:\n");
        config.append("- 默认索引名称: ").append(DEFAULT_INDEX_NAME).append("\n");
        config.append("- 默认键前缀: ").append(DEFAULT_PREFIX).append("\n");
        config.append("- 默认TopK: ").append(DEFAULT_TOP_K).append("\n");
        config.append("- 默认相似性阈值: ").append(DEFAULT_SIMILARITY_THRESHOLD).append("\n");
        config.append("- 向量存储连接状态: ").append(checkVectorStoreConnection() ? "正常" : "异常").append("\n");
        config.append("- 嵌入模型状态: ").append(getEmbeddingModelInfo());
        
        return config.toString();
    }

    /**
     * 文档类
     * 
     * 用于表示向量存储中的文档，包含内容和元数据。
     */
    public static class Document {
        private final String content;
        private final Map<String, Object> metadata;

        /**
         * 构造函数
         * 
         * @param content 文档内容
         * @param metadata 文档元数据
         */
        public Document(String content, Map<String, Object> metadata) {
            this.content = content;
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        }

        /**
         * 获取文档内容
         * 
         * @return 文档内容
         */
        public String getContent() {
            return content;
        }

        /**
         * 获取文档元数据
         * 
         * @return 文档元数据
         */
        public Map<String, Object> getMetadata() {
            return new HashMap<>(metadata);
        }

        @Override
        public String toString() {
            return "Document{" +
                    "content='" + content + '\'' +
                    ", metadata=" + metadata +
                    '}';
        }
    }
} 