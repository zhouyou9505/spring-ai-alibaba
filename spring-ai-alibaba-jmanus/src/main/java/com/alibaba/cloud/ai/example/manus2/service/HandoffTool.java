package com.alibaba.cloud.ai.example.manus2.service;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Agent转交工具类
 * 实现LangGraph的handoff思想，支持agent之间的顺序转交
 */
@Slf4j
@Service
public class HandoffTool {

    @Autowired
    private AgentsService agentsService;

    @Autowired
    private CoreService coreService;


    public static void setHandoffs(ReactAgent newAgent, List<ReactAgent> handoffs) {

    }
}
