package com.alibaba.cloud.ai.example.manus2.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring AI配置类
 * 
 * 该类配置了Spring AI的相关组件，包括：
 * - Redis连接配置
 * - 基础配置框架
 * 
 * @author Rowboat Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Configuration
public class SpringAiConfig {

    private static final Logger logger = LoggerFactory.getLogger(SpringAiConfig.class);

    /**
     * OpenAI API密钥
     */
    @Value("${spring.ai.openai.api-key:}")
    private String openaiApiKey;

    /**
     * OpenAI API基础URL
     */
    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String openaiBaseUrl;

    /**
     * Redis主机地址
     */
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    /**
     * Redis端口
     */
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    /**
     * Redis用户名
     */
    @Value("${spring.data.redis.username:}")
    private String redisUsername;

    /**
     * Redis密码
     */
    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * 向量存储索引名称
     */
    @Value("${spring.ai.vectorstore.redis.index-name:rowboat-ai-index}")
    private String indexName;

    /**
     * Redis键前缀
     */
    @Value("${spring.ai.vectorstore.redis.prefix:embedding:}")
    private String prefix;

    /**
     * 是否初始化向量存储模式
     */
    @Value("${spring.ai.vectorstore.redis.initialize-schema:true}")
    private boolean initializeSchema;

    /**
     * 配置Redis模板
     * 
     * @param connectionFactory Redis连接工厂
     * @return 配置好的Redis模板
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        logger.info("配置Redis模板");
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 设置序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        
        template.afterPropertiesSet();
        logger.info("Redis模板配置完成");
        
        return template;
    }

    /**
     * 配置Jedis连接池
     * 
     * @return JedisPooled实例
     */
    @Bean
    public JedisPooled jedisPooled() {
        logger.info("配置Jedis连接池，主机: {}, 端口: {}", redisHost, redisPort);
        
        JedisPooled jedisPooled;
        if (redisUsername != null && !redisUsername.trim().isEmpty() && 
            redisPassword != null && !redisPassword.trim().isEmpty()) {
            jedisPooled = new JedisPooled(redisHost, redisPort, redisUsername, redisPassword);
        } else if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            jedisPooled = new JedisPooled(redisHost, redisPort, "",redisPassword);
        } else {
            jedisPooled = new JedisPooled(redisHost, redisPort);
        }
        
        logger.info("Jedis连接池配置完成");
        return jedisPooled;
    }

    /**
     * 获取配置信息
     * 
     * @return 配置信息字符串
     */
    public String getConfigurationInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Spring AI配置信息:\n");
        info.append("- OpenAI基础URL: ").append(openaiBaseUrl).append("\n");
        info.append("- OpenAI API密钥: ").append(openaiApiKey != null && !openaiApiKey.isEmpty() ? "已配置" : "未配置").append("\n");
        info.append("- Redis主机: ").append(redisHost).append("\n");
        info.append("- Redis端口: ").append(redisPort).append("\n");
        info.append("- 向量存储索引名称: ").append(indexName).append("\n");
        info.append("- Redis键前缀: ").append(prefix).append("\n");
        info.append("- 初始化模式: ").append(initializeSchema).append("\n");
        
        return info.toString();
    }
} 