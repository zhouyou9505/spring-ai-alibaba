package com.alibaba.cloud.ai.example.manus2.tools;

import com.alibaba.cloud.ai.toolcalling.tavily.TavilySearchService;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class WebSearchTool {

    @Resource
    private TavilySearchService tavilySearchService;

    @Tool(description = "搜索网络信息", name = "web_search")
    public TavilySearchService.Response web_search(@ToolParam TavilySearchService.Request request){
        return tavilySearchService.apply(request);
    }

}
