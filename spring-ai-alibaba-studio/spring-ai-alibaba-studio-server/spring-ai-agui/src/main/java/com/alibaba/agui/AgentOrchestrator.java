package com.alibaba.agui;

import com.alibaba.cloud.ai.graph.event.agent.RunAgentInput;
import com.alibaba.cloud.ai.graph.event.event.BaseEvent;
import com.alibaba.cloud.ai.graph.event.event.RunErrorEvent;
import com.alibaba.cloud.ai.graph.event.event.RunFinishedEvent;
import com.alibaba.cloud.ai.graph.event.event.RunStartedEvent;
import com.alibaba.cloud.ai.graph.event.manager.CallbackManager;
import com.alibaba.cloud.ai.graph.event.manager.CallbackManagerImpl;
import com.alibaba.cloud.ai.graph.event.manager.EventHandler;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    public AgentOrchestrator() {}

    /**
     * 包一层生命周期：RUN_STARTED → (代理执行) → RUN_FINISHED / RUN_ERROR
     * 并以 SSE 的 event: message 逐条输出。
     */
    public Flux<ServerSentEvent<BaseEvent>> run(Consumer<CallbackManager> consumer, RunAgentInput input) {

        return Flux.create(emitter -> {
            // 事件处理：每次收到 BaseEvent，就作为 SSE 推出去
            EventHandler handler = new EventHandler(event -> {
                try {
                    ServerSentEvent<BaseEvent> sse = ServerSentEvent.<BaseEvent>builder()
                            .event("message")                 // ← CopilotKit/AG-UI 约定
                            .id(input.threadId()) // 唯一 id，方便客户端去重
                            .data(event)                      // 序列化为 data: {...}
                            .build();
                    log.info(JSON.toJSONString(event));
                    emitter.next(sse);
                } catch (Exception ex) {
                    emitter.error(ex);
                }
            });

            CallbackManager callbackManager = new CallbackManagerImpl(handler);

            try {
                // 1) RUN_STARTED
                emitRunStarted(callbackManager, input);

                // 2) 这里可选：发送 STATE_SNAPSHOT（如果你的 CallbackManager 支持）
                // callbackManager.onStateSnapshot(state);

                // 3) 真正执行（异步）
                CompletableFuture.runAsync(() -> {
                    try {
                        consumer.accept(callbackManager);

                        // 4) RUN_FINISHED
                        emitRunFinished(callbackManager, input);
                        emitter.complete();

                    } catch (Exception execEx) {
                        // 5) RUN_ERROR
                        emitRunError(callbackManager, execEx.getMessage());
                        emitter.error(execEx);
                    }
                });

            } catch (Exception e) {
                emitRunError(callbackManager, e.getMessage());
                emitter.error(e);
            }
        });
    }

    private void emitRunStarted(CallbackManager cb, RunAgentInput in) {
        RunStartedEvent ev = new RunStartedEvent();
        ev.setThreadId(in.threadId());
        ev.setRunId(in.runId());
        ev.setTimestamp(Instant.now().toEpochMilli());
        cb.onRunStartedEvent(ev);
    }

    private void emitRunFinished(CallbackManager cb, RunAgentInput in) {
        RunFinishedEvent ev = new RunFinishedEvent();
        ev.setThreadId(in.threadId());
        ev.setRunId(in.runId());
        ev.setResult(null); // 如需带汇总结果，这里可以放 result 对象
        ev.setTimestamp(Instant.now().toEpochMilli());

        cb.onRunFinishedEvent(ev);
    }

    private void emitRunError(CallbackManager cb, String msg) {
        RunErrorEvent ev = new RunErrorEvent();
        ev.setError(msg);
        ev.setTimestamp(Instant.now().toEpochMilli());

        cb.onRunErrorEvent(ev);
    }
}
