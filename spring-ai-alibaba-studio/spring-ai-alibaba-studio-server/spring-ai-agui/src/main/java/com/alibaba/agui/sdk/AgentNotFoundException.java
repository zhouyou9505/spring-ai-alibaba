package com.alibaba.agui.sdk;

public class AgentNotFoundException extends RuntimeException {
    private final String name;
    public AgentNotFoundException(String name) {
        super("Agent '" + name + "' not found.");
        this.name = name;
    }
    public String getName() { return name; }
}