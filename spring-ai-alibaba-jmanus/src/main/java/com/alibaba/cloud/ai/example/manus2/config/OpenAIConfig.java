/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.example.manus2.config;

import lombok.Data;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Configuration
public class OpenAIConfig {

    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;


    @Value("${spring.ai.openai.temperature:0.6}")
    private Double temperature;


    @Bean
    public ChatClient chatClient(@Autowired OpenAiChatModel openAiChatModel) {
        return ChatClient.create(openAiChatModel);
    }



    @Bean
    public ChatClient copilotChatClient(@Autowired OpenAiChatModel openAiChatModel) {
        return ChatClient.create(openAiChatModel);
    }
} 