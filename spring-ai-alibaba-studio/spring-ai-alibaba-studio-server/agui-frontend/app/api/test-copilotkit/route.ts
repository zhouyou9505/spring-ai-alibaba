import { NextRequest, NextResponse } from "next/server";

export async function GET(req: NextRequest) {
  try {
    console.log("Testing CopilotKit backend integration");
    
    const backendUrl = process.env.BACKEND_URL || "http://localhost:8080";
    
    // Test 1: Check /info endpoint
    console.log("Testing /info endpoint...");
    const infoResponse = await fetch(`${backendUrl}/copilotkit/info`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({}),
    });
    
    const infoData = await infoResponse.json();
    console.log("Info response:", infoData);
    
    // Test 2: Check /agents/state endpoint
    console.log("Testing /agents/state endpoint...");
    const stateResponse = await fetch(`${backendUrl}/copilotkit/agents/state`, {
      method: "POST", 
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        threadId: "test-thread-123",
        name: "ai_researcher",
        properties: {}
      }),
    });
    
    const stateData = await stateResponse.json();
    console.log("State response:", stateData);
    
    // Test 3: Validate response formats
    const validation = {
      info: {
        success: infoResponse.ok,
        hasAgents: Array.isArray(infoData.agents),
        hasActions: Array.isArray(infoData.actions),
        agentCount: infoData.agents?.length || 0,
        actionCount: infoData.actions?.length || 0,
      },
      state: {
        success: stateResponse.ok,
        hasThreadId: typeof stateData.threadId === 'string' && stateData.threadId !== null,
        hasThreadExists: typeof stateData.threadExists === 'boolean',
        hasState: typeof stateData.state === 'string',
        hasMessages: typeof stateData.messages === 'string',
        threadIdValue: stateData.threadId,
      }
    };
    
    return NextResponse.json({
      success: true,
      timestamp: new Date().toISOString(),
      backend: {
        url: backendUrl,
        info: {
          status: infoResponse.status,
          data: infoData
        },
        state: {
          status: stateResponse.status,
          data: stateData
        }
      },
      validation,
      copilotkit: {
        runtimeUrl: "/api/copilotkit",
        agent: "ai_researcher",
        backendConnected: infoResponse.ok && stateResponse.ok
      }
    });
    
  } catch (error: any) {
    console.error("CopilotKit test failed:", error);
    return NextResponse.json({
      success: false,
      error: error.message,
      stack: error.stack,
      timestamp: new Date().toISOString(),
    });
  }
}