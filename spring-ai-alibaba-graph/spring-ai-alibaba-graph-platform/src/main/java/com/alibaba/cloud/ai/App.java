package com.alibaba.cloud.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring AI Alibaba Graph Platform 启动类
 *
 * @author AI Assistant
 */
@SpringBootApplication(scanBasePackages = "com.alibaba.cloud.ai")
public class App 
{
    public static void main( String[] args )
    {
        SpringApplication.run(App.class, args);
    }
}
