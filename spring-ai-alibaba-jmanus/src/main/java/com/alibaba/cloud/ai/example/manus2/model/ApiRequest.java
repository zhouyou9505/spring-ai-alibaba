package com.alibaba.cloud.ai.example.manus2.model;

import com.alibaba.cloud.ai.example.manus2.model.context.Context;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * API请求模型
 * 
 * 工作流配置示例:
 * [
 *   "12ef9853-e438-4782-80b8-78adf1c717ce",  // projectId
 *   "684e1e2fbd32b4c3bcf3cc0d",              // workflowId
 *   {
 *     "projectId": "12ef9853-e438-4782-80b8-78adf1c717ce",
 *     "agents": [
 *       {
 *         "name": "Courier Hub",
 *         "type": "conversation",
 *         "description": "Main hub agent for courier and delivery assistance",
 *         "instructions": "## 🧑‍💼 Role:\nYou are the main hub for courier and delivery assistance.\n\n---\n## ⚙️ Steps to Follow:\n1. Greet the user and ask how you can help with their courier needs.\n2. If the user communicates in Chinese characters → Call [@agent:Delivery Agent](#mention)\n3. Otherwise, based on their request:\n   - For tracking inquiries → Call [@agent:Tracking Agent](#mention)\n   - For delivery issues → Call [@agent:Delivery Issues Agent](#mention)\n   - For service information → Call [@agent:Service Info Agent](#mention)\n4. If the request doesn't match any category, politely inform the user you can only assist with courier-related matters.\n\n---\n## 🎯 Scope:\n✅ In Scope:\n- Package tracking\n- Delivery issues\n- Service information\n\n❌ Out of Scope:\n- Non-courier related requests\n\n---\n## 📋 Guidelines:\n✔️ Dos:\n- Route requests to appropriate agents\n- Detect Chinese language and route to Delivery Agent\n- Keep interactions brief and to the point\n\n🚫 Don'ts:\n- Don't handle requests outside courier services\n- Don't provide user-facing text such as 'I will connect you now...' when calling another agent",
 *         "model": "deepseek/deepseek-chat-v3-0324:free",
 *         "toggleAble": true,
 *         "ragReturnType": "chunks",
 *         "ragK": 3,
 *         "controlType": "retain",
 *         "outputVisibility": "user_facing",
 *         "examples": "- **User** : 我的快递在哪\n - **Agent actions**: Call [@agent:Delivery Agent](#mention)\n\n- **User** : I need help tracking my package\n - **Agent actions**: Call [@agent:Tracking Agent](#mention)\n\n- **User** : My delivery was damaged\n - **Agent actions**: Call [@agent:Delivery Issues Agent](#mention)\n\n- **User** : What delivery options do you have?\n - **Agent actions**: Call [@agent:Service Info Agent](#mention)\n\n- **User** : How's the weather today?\n - **Agent response**: I can only assist with courier-related matters. How can I help you with your delivery needs?",
 *         "order": 200
 *       },
 *       {
 *         "name": "Delivery Agent",
 *         "type": "conversation",
 *         "description": "Chinese-language courier assistant for package tracking, delivery issues, and service information.",
 *         "disabled": false,
 *         "instructions": "## 🧑‍💼 Role:\nYou are a courier assistant specialized in helping users with their delivery needs in Chinese.\n\n---\n## ⚙️ Steps to Follow:\n1. Greet the user in Chinese (e.g., '您好，请问有什么快递服务可以帮您？').\n2. For tracking inquiries (phrases like '我的快递在哪', '查快递', '快递查询', '物流信息'):\n   - Ask for tracking number if not provided\n   - Call [@agent:Tracking Agent](#mention) with tracking details\n3. For other requests:\n   - Delivery issues → Call [@agent:Delivery Issues Agent](#mention)\n   - Service information → Call [@agent:Service Info Agent](#mention)\n4. If request is unclear, ask clarifying questions in Chinese.\n\n---\n## 🎯 Scope:\n✅ In Scope:\n- Package tracking\n- Delivery issues\n- Service information\n\n❌ Out of Scope:\n- Non-courier related requests\n\n---\n## 📋 Guidelines:\n✔️ Dos:\n- Use polite Chinese business language\n- Confirm tracking numbers when needed\n- Route requests appropriately\n\n🚫 Don'ts:\n- Don't switch to English\n- Don't handle non-courier requests",
 *         "model": "deepseek/deepseek-chat-v3-0324:free",
 *         "locked": false,
 *         "toggleAble": true,
 *         "ragReturnType": "chunks",
 *         "ragK": 3,
 *         "controlType": "retain",
 *         "outputVisibility": "user_facing",
 *         "maxCallsPerParentAgent": 3,
 *         "examples": "- **User** : 我的快递在哪\n - **Agent response**: 请问您的快递单号是多少？\n\n- **User** : 查快递\n - **Agent response**: 请提供您的快递单号以便查询。\n\n- **User** : 快递查询 123456789\n - **Agent actions**: Call [@agent:Tracking Agent](#mention)\n\n- **User** : 我的包裹损坏了\n - **Agent actions**: Call [@agent:Delivery Issues Agent](#mention)\n\n- **User** : 配送方式有哪些\n - **Agent actions**: Call [@agent:Service Info Agent](#mention)"
 *       }
 *     ],
 *     "prompts": [],
 *     "tools": [
 *       {
 *         "name": "rag_search",
 *         "description": "Fetch articles with knowledge relevant to the query",
 *         "parameters": {
 *           "type": "object",
 *           "properties": {
 *             "query": {
 *               "type": "string",
 *               "description": "The query to retrieve articles for"
 *             }
 *           },
 *           "required": ["query"]
 *         },
 *         "isLibrary": true
 *       }
 *     ],
 *     "startAgent": "Courier Hub",
 *     "createdAt": "2025-06-15T01:13:19.993Z",
 *     "lastUpdatedAt": "2025-06-15T02:24:03.858Z",
 *     "name": "Version 1",
 *     "_id": "684e1e2fbd32b4c3bcf3cc0d"
 *   }
 * ]
 */
@Data
public class ApiRequest {

    private List<Message> messages;
    
    @JsonProperty("workflow_schema")
    private String workflowSchema;
    
    @JsonProperty("current_workflow_config")
    private String currentWorkflowConfig;
    
    private Context context;
    
    @JsonProperty("data_sources")
    private List<DataSource> dataSources;

} 