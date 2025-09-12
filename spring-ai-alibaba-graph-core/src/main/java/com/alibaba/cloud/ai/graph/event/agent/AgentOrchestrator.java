package com.alibaba.cloud.ai.graph.event.agent;

import com.alibaba.cloud.ai.graph.event.event.BaseEvent;
import com.alibaba.cloud.ai.graph.event.event.RunErrorEvent;
import com.alibaba.cloud.ai.graph.event.event.RunFinishedEvent;
import com.alibaba.cloud.ai.graph.event.event.RunStartedEvent;
import com.alibaba.cloud.ai.graph.event.manager.CallbackManager;
import com.alibaba.cloud.ai.graph.event.manager.CallbackManagerImpl;
import com.alibaba.cloud.ai.graph.event.manager.EventHandler;
import com.alibaba.fastjson.JSON;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    public AgentOrchestrator() {}

    public Flux<ServerSentEvent<BaseEvent>> run(Consumer<CallbackManager> consumer, RunAgentInput input) {

        return Flux.create(emitter -> {
            CallbackManager callbackManager = getCallbackManager(input, emitter);

            try {
                emitRunStarted(callbackManager, input);

                CompletableFuture.runAsync(() -> {
                    try {
                        consumer.accept(callbackManager);

                        emitRunFinished(callbackManager, input);
                        emitter.complete();

                    } catch (Exception execEx) {
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

    private static @NotNull CallbackManager getCallbackManager(RunAgentInput input, FluxSink<ServerSentEvent<BaseEvent>> emitter) {
        EventHandler handler = new EventHandler(event -> {
            try {
                ServerSentEvent<BaseEvent> sse = ServerSentEvent.<BaseEvent>builder()
                        .event("message")
                        .id(input.threadId())
                        .data(event)
                        .build();
                log.info(JSON.toJSONString(event));
                emitter.next(sse);
            } catch (Exception ex) {
                emitter.error(ex);
            }
        });

        CallbackManager callbackManager = new CallbackManagerImpl(handler);
        return callbackManager;
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
        ev.setResult(null);
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
