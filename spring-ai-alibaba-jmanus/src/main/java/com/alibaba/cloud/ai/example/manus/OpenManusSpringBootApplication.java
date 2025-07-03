/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.example.manus;

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
@ComponentScan(basePackages = {"com.alibaba.cloud.ai.example.manus","com.alibaba.cloud.ai.example.manus2"})
@EntityScan(basePackages = {"com.alibaba.cloud.ai.example.manus","com.alibaba.cloud.ai.example.manus2"})
@EnableMongoRepositories(basePackages = {"com.alibaba.cloud.ai.example.manus2"})
public class OpenManusSpringBootApplication {

	public static void main(String[] args) {

		SpringApplication.run(OpenManusSpringBootApplication.class, args);
	}

}
