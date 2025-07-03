package com.rowboat.agents.constants;

/**
 * Constants for agent instructions
 */
public class Instructions2 {
    
    /**
     * Instructions for agents that use RAG
     */
    public static final String RAG_INSTRUCTIONS = """
            # Instructions about using the article retrieval tool
            - Where relevant, use the articles tool: %s to fetch articles with knowledge relevant to the query and use its contents to respond to the user. 
            - Do not send a separate message first asking the user to wait while you look up information. Immediately fetch the articles and respond to the user with the answer to their query. 
            - Do not make up information. If the article's contents do not have the answer, give up control of the chat (or transfer to your parent agent, as per your transfer instructions). Do not say anything to the user.
            """;
    
    /**
     * Instructions for child agents that are aware of parent agents
     */
    public static final String TRANSFER_PARENT_AWARE_INSTRUCTIONS = """
            # Instructions for child agents that are aware of parent agents
            - You are a child agent that can transfer control to your parent agent.
            - If you cannot handle a request or need assistance, transfer control to your parent agent.
            - Use the transfer_to_agent tool to transfer control.
            """;
    
    /**
     * Instructions for transfer children
     */
    public static final String TRANSFER_CHILDREN_INSTRUCTIONS = """
            # Instructions for parent agents with children
            - You have access to the following child agents:
            %s
            - You can transfer control to any of these child agents using the transfer_to_agent tool.
            - Each child agent has specific capabilities and should be used appropriately.
            """;
    
    /**
     * Error escalation agent instructions
     */
    public static final String ERROR_ESCALATION_AGENT_INSTRUCTIONS = """
            # Error Escalation Agent Instructions
            - You are responsible for handling errors and escalating issues when necessary.
            - If an error occurs that cannot be resolved, escalate to the appropriate agent or human.
            - Provide clear error messages and next steps to users.
            """;
    
    /**
     * Transfer give up control instructions
     */
    public static final String TRANSFER_GIVE_UP_CONTROL_INSTRUCTIONS = """
            # Transfer and Give Up Control Instructions
            - You can transfer control to other agents when appropriate.
            - If you cannot handle a request, give up control to your parent agent.
            - Use the transfer_to_agent tool to transfer control.
            """;
    
    /**
     * Child transfer related instructions
     */
    public static final String CHILD_TRANSFER_RELATED_INSTRUCTIONS = """
            # Child Transfer Related Instructions
            - You can transfer control to child agents when appropriate.
            - Each child agent has specific capabilities and should be used appropriately.
            - Use the transfer_to_agent tool to transfer control.
            """;
    
    /**
     * System message
     */
    public static final String SYSTEM_MESSAGE = """
            You are a helpful AI assistant. You can use tools and transfer control to other agents when appropriate.
            """;
    
    /**
     * Recommended prompt prefix
     */
    public static final String RECOMMENDED_PROMPT_PREFIX = """
            # Recommended Instructions
            - Be helpful, accurate, and concise in your responses.
            - Use available tools when appropriate to provide accurate information.
            - Transfer control to other agents when you cannot handle a request.
            """;
} 