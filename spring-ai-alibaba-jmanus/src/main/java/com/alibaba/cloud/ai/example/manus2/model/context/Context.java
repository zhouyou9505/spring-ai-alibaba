package com.alibaba.cloud.ai.example.manus2.model.context;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AgentContext.class, name = "agent"),
    @JsonSubTypes.Type(value = PromptContext.class, name = "prompt"),
    @JsonSubTypes.Type(value = ToolContext.class, name = "tool"),
    @JsonSubTypes.Type(value = ChatContext.class, name = "chat")
})
public abstract class Context {
    private String type;
}

