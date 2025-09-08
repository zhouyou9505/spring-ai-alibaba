//package com.alibaba.agui;/*
// * Copyright 2024-2025 the original author or authors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//import com.alibaba.cloud.ai.graph.event.agent.RunAgentInput;
//import com.alibaba.cloud.ai.graph.event.event.BaseEvent;
//import com.alibaba.cloud.ai.graph.event.event.RunErrorEvent;
//import com.alibaba.cloud.ai.graph.event.event.RunFinishedEvent;
//import com.alibaba.cloud.ai.graph.event.event.RunStartedEvent;
//import com.alibaba.cloud.ai.graph.event.manager.CallbackManager;
//import com.alibaba.cloud.ai.graph.event.manager.CallbackManagerImpl;
//import com.alibaba.cloud.ai.graph.event.manager.EventHandler;
//import com.alibaba.fastjson.JSON;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.codec.ServerSentEvent;
//import reactor.core.publisher.Flux;
//
//import java.util.concurrent.CompletableFuture;
//import java.util.function.Consumer;
//
///**
// * EventAgent provides lifecycle event management around agent execution.
// * <p>
// * This class wraps agent execution with proper event emission for run lifecycle:
// * RUN_STARTED → agent execution → RUN_FINISHED (or RUN_ERROR on failure)
// * </p>
// */
//@Slf4j
//public class AgentOrchestrator {
//
//
//    public AgentOrchestrator() {
//
//    }
//
//    /**
//     * Execute agent with lifecycle event management and built-in ReactAgent creation.
//     *
//     * @param input the agent input parameters containing thread and run information
//     */
//    public Flux<ServerSentEvent<BaseEvent>> run(Consumer<CallbackManager> consumer, RunAgentInput input) {
//
//        return Flux.create(emitter -> {
//            EventHandler eventHandler = new EventHandler(event -> {
//                try {
//                    ServerSentEvent<BaseEvent> sseEvent = ServerSentEvent.<BaseEvent>builder()
//                            .id(input.threadId())
//                            .data(event)
//                            .build();
//                    emitter.next(sseEvent);
//                    System.out.println(JSON.toJSONString(event));
//                } catch (Exception e) {
//                    emitter.error(e);
//                }
//            });
//            CallbackManager callbackManager = new CallbackManagerImpl(eventHandler);
//            try {
//                // Emit RUN_STARTED event
//                emitRunStartedEvent(callbackManager,input);
//
//                // Execute agent asynchronously
//                CompletableFuture.runAsync(() -> {
//                    try {
//
//                        consumer.accept(callbackManager);
//
//                        // Emit RUN_FINISHED event
//                        emitRunFinishedEvent(callbackManager,input);
//                        emitter.complete();
//
//                    } catch (Exception e) {
//                        emitter.error(e);
//                    }
//                });
//
//            } catch (Exception e) {
//                // Emit RUN_ERROR event
//                emitRunErrorEvent(callbackManager,e.getMessage());
//                emitter.error(e);
//            }
//        });
//
//    }
//
//
//
//
//    private void emitRunStartedEvent(CallbackManager callbackManager, RunAgentInput input) {
//        RunStartedEvent event = new RunStartedEvent();
//        event.setThreadId(input.threadId());
//        event.setRunId(input.runId());
//        callbackManager.onRunStartedEvent(event);
//
//    }
//
//    private void emitRunFinishedEvent(CallbackManager callbackManager, RunAgentInput input) {
//        RunFinishedEvent event = new RunFinishedEvent();
//        event.setThreadId(input.threadId());
//        event.setRunId(input.runId());
//        event.setResult(null); // Result handling can be enhanced if needed
//        callbackManager.onRunFinishedEvent(event);
//
//    }
//
//    private void emitRunErrorEvent(CallbackManager callbackManager, String errorMessage) {
//        RunErrorEvent event = new RunErrorEvent();
//        event.setError(errorMessage);
//        callbackManager.onRunErrorEvent(event);
//    }
//}