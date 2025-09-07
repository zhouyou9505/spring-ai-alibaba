package com.alibaba.agui.sdk;

/**
 * 自定义异常：Action 没找到
 */
public class ActionNotFoundException extends RuntimeException {
    private final String name;
    public ActionNotFoundException(String name) {
        super("Action '" + name + "' not found.");
        this.name = name;
    }
    public String getName() { return name; }
}
