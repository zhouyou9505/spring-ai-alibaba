package com.alibaba.agui.sdk;// package com.example.copilotkit.sdk;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface Action {
  String name();
  Map<String, Object> dictRepr();  // name/description/parameters
  Object execute(Map<String, Object> args) throws Exception;
}

