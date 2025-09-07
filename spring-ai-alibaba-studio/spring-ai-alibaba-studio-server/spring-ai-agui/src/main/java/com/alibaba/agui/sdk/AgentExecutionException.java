package com.alibaba.agui.sdk;

/**
 * 自定义异常：Agent 执行失败
 */
public class AgentExecutionException extends RuntimeException {
    private final String name;
    private final Throwable error;
    public AgentExecutionException(String name, Throwable error) {
        super("Agent '" + name + "' failed to execute: " + error, error);
        this.name = name;
        this.error = error;
    }
    public String getName() { return name; }
    public Throwable getError() { return error; }
}
