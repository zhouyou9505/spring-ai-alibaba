package com.alibaba.agui;

import java.util.Map;

// package com.example.copilotkit.adapter;
public final class CopilotKitNdjson {
  private static final com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
  public static String encode(Map<String,Object> evt){
    try { return om.writeValueAsString(evt) + "\n"; }
    catch(Exception e){ return "{\"event\":\"on_copilotkit_error\",\"data\":{\"message\":\""+e.getMessage()+"\"}}\n"; }
  }
}
