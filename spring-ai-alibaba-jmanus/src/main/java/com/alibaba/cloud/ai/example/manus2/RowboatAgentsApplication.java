package com.alibaba.cloud.ai.example.manus2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableMongoAuditing
@EnableAsync
@ComponentScan(basePackages = {"com.rowboat"})
@EntityScan(basePackages = {"com.rowboat"})
@EnableMongoRepositories(basePackages = {"com.rowboat"})
public class RowboatAgentsApplication {
    public static void main(String[] args) {
        SpringApplication.run(RowboatAgentsApplication.class, args);
    }
} 