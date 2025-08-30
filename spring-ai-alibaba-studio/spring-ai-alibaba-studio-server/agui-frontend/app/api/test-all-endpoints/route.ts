import { NextRequest, NextResponse } from "next/server";

export async function GET(req: NextRequest) {
  try {
    console.log("Testing all CopilotKit backend endpoints");
    
    const backendUrl = process.env.BACKEND_URL || "http://localhost:8080";
    const results: any = {};
    
    // Test 1: Health endpoint
    console.log("Testing health endpoint...");
    try {
      const healthResponse = await fetch(`${backendUrl}/copilotkit/health`, {
        method: "GET",
      });
      results.health = {
        status: healthResponse.status,
        available: healthResponse.ok,
        error: healthResponse.ok ? null : await healthResponse.text()
      };
    } catch (error: any) {
      results.health = {
        status: 'error',
        available: false,
        error: error.message
      };
    }
    
    // Test 2: Info endpoint
    console.log("Testing info endpoint...");
    try {
      const infoResponse = await fetch(`${backendUrl}/copilotkit/info`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({})
      });
      results.info = {
        status: infoResponse.status,
        available: infoResponse.ok,
        data: infoResponse.ok ? await infoResponse.json() : null,
        error: infoResponse.ok ? null : await infoResponse.text()
      };
    } catch (error: any) {
      results.info = {
        status: 'error',
        available: false,
        error: error.message
      };
    }
    
    // Test 3: Agents/state endpoint
    console.log("Testing agents/state endpoint...");
    try {
      const stateResponse = await fetch(`${backendUrl}/copilotkit/agents/state`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          threadId: "test-123",
          name: "ai_researcher"
        })
      });
      results.agentsState = {
        status: stateResponse.status,
        available: stateResponse.ok,
        data: stateResponse.ok ? await stateResponse.json() : null,
        error: stateResponse.ok ? null : await stateResponse.text()
      };
    } catch (error: any) {
      results.agentsState = {
        status: 'error',
        available: false,
        error: error.message
      };
    }
    
    // Test 4: Agents/execute endpoint (just check if it responds, not the stream)
    console.log("Testing agents/execute endpoint...");
    try {
      const executeResponse = await fetch(`${backendUrl}/copilotkit/agents/execute`, {
        method: "POST",
        headers: { 
          "Content-Type": "application/json",
          "Accept": "text/event-stream"
        },
        body: JSON.stringify({
          threadId: "test-exec-123",
          name: "ai_researcher",
          state: {},
          messages: [],
          actions: []
        })
      });
      results.agentsExecute = {
        status: executeResponse.status,
        available: executeResponse.ok,
        headers: Object.fromEntries(executeResponse.headers.entries()),
        error: executeResponse.ok ? null : await executeResponse.text()
      };
    } catch (error: any) {
      results.agentsExecute = {
        status: 'error',
        available: false,
        error: error.message
      };
    }
    
    // Test 5: MCP Tools Discovery endpoint
    console.log("Testing mcp/tools endpoint...");
    try {
      const mcpResponse = await fetch(`${backendUrl}/copilotkit/mcp/tools`, {
        method: "GET",
        headers: { "Content-Type": "application/json" }
      });
      results.mcpTools = {
        status: mcpResponse.status,
        available: mcpResponse.ok,
        data: mcpResponse.ok ? await mcpResponse.json() : null,
        error: mcpResponse.ok ? null : await mcpResponse.text()
      };
    } catch (error: any) {
      results.mcpTools = {
        status: 'error',
        available: false,
        error: error.message
      };
    }
    
    // Summary
    const availableEndpoints = Object.entries(results)
      .filter(([_, result]: [string, any]) => result.available)
      .map(([name, _]) => name);
      
    const unavailableEndpoints = Object.entries(results)
      .filter(([_, result]: [string, any]) => !result.available)
      .map(([name, _]) => name);
    
    // Protocol Compliance Analysis
    const protocolCompliance = {
      copilotKit: results.info?.available && results.agentsState?.available,
      agui: results.agentsExecute?.available,
      mcp: results.mcpTools?.available,
      fullIntegration: availableEndpoints.length === Object.keys(results).length
    };
    
    return NextResponse.json({
      success: true,
      backendUrl,
      
      // Enhanced Summary
      summary: {
        availableEndpoints,
        unavailableEndpoints,
        totalTested: Object.keys(results).length,
        allWorking: unavailableEndpoints.length === 0,
        protocolCompliance
      },
      
      // Detailed Results
      details: results,
      
      // Integration Status
      integrationStatus: {
        copilotKitReady: results.info?.available && results.agentsState?.available,
        aguiProtocolReady: results.agentsExecute?.available,
        mcpProtocolReady: results.mcpTools?.available,
        fullStackReady: unavailableEndpoints.length === 0
      },
      
      timestamp: new Date().toISOString(),
      
      // Recommendations
      recommendation: unavailableEndpoints.length > 0 ? 
        "Backend needs to be restarted to apply new endpoint implementations" : 
        "All protocols (CopilotKit + AG-UI + MCP) are working correctly"
    });
    
  } catch (error: any) {
    console.error("Endpoint test failed:", error);
    return NextResponse.json({
      success: false,
      error: error.message,
      recommendation: "Check if Spring Boot backend is running on port 8080"
    });
  }
}