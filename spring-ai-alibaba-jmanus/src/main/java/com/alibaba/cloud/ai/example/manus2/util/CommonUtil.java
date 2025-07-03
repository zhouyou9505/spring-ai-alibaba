package com.alibaba.cloud.ai.example.manus2.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CommonUtil {
    
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    
    @Value("${api.key:}")
    private String apiKey;
    
    public static Map<String, Object> readJsonFromFile(String fileName) {
        log.info("Reading json from {}", fileName);
        try {
            String content = new String(Files.readAllBytes(Paths.get(fileName)));
            return objectMapper.readValue(content, Map.class);
        } catch (IOException e) {
            log.error("Error reading JSON file: {}", e.getMessage(), e);
            return null;
        }
    }
    
    public static String readTextFromFile(String fileName) {
        try {
            return new String(Files.readAllBytes(Paths.get(fileName)));
        } catch (IOException e) {
            log.error("Error reading text file: {}", e.getMessage(), e);
            return null;
        }
    }
    
    public static boolean writeJsonToFile(Map<String, Object> data, String fileName) {
        try {
            objectMapper.writeValue(new File(fileName), data);
            return true;
        } catch (IOException e) {
            log.error("Error writing JSON file: {}", e.getMessage(), e);
            return false;
        }
    }
    
    public static List<Map<String, Object>> readJsonlFromFile(String fileName) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(fileName));
            return lines.stream()
                    .map(line -> {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> result = objectMapper.readValue(line, Map.class);
                            return result;
                        } catch (IOException e) {
                            log.error("Error parsing JSONL line: {}", e.getMessage());
                            return null;
                        }
                    })
                    .filter(map -> map != null)
                    .toList();
        } catch (IOException e) {
            log.error("Error reading JSONL file: {}", e.getMessage(), e);
            return null;
        }
    }
    
    public static boolean writeJsonlToFile(List<Map<String, Object>> listDicts, String fileName) {
        try {
            List<String> lines = listDicts.stream()
                    .map(dict -> {
                        try {
                            return objectMapper.writeValueAsString(dict);
                        } catch (IOException e) {
                            log.error("Error serializing JSONL item: {}", e.getMessage());
                            return null;
                        }
                    })
                    .filter(line -> line != null)
                    .toList();
            
            Files.write(Paths.get(fileName), lines);
            return true;
        } catch (IOException e) {
            log.error("Error writing JSONL file: {}", e.getMessage(), e);
            return false;
        }
    }
    
    public String getApiKey(String keyName) {
        String apiKeyValue = System.getenv(keyName);
        if (apiKeyValue == null || apiKeyValue.trim().isEmpty()) {
            throw new IllegalArgumentException(keyName + " not found. Did you set it in the environment?");
        }
        return apiKeyValue;
    }
} 