package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.toolcalling.baidusearch.BaiduSearchService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SearchTool {

    @Autowired
    private BaiduSearchService baiduSearchService;

    @Tool(name = "web_search")
    public BaiduSearchService.Response web_search(@ToolParam BaiduSearchService.Request query) {
        return baiduSearchService.apply(query);
    }
}
