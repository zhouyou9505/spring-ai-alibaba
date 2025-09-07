package com.alibaba.agui.sdk;

public class ActionExecutionException extends RuntimeException {
    private final String name;
    private final Throwable error;
    public ActionExecutionException(String name, Throwable error) {
        super("Action '" + name + "' failed to execute: " + error, error);
        this.name = name;
        this.error = error;
    }
    public String getName() { return name; }
    public Throwable getError() { return error; }
}