import { NextRequest } from "next/server";

/**
 * Test endpoint to check the backend /info response directly
 */
export const GET = async (req: NextRequest) => {
  try {
    const backendUrl = process.env.BACKEND_URL || "http://localhost:8080";
    
    console.log("Testing backend /info endpoint directly...");
    
    // Test both GET and POST methods
    const results = await Promise.allSettled([
      // Test GET method
      fetch(`${backendUrl}/copilotkit/info`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
        },
      }),
      // Test POST method  
      fetch(`${backendUrl}/copilotkit/info`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ properties: {}, frontendUrl: "http://localhost:3000" }),
      })
    ]);
    
    const responses = await Promise.all(
      results.map(async (result, index) => {
        const method = index === 0 ? "GET" : "POST";
        if (result.status === "fulfilled" && result.value.ok) {
          const data = await result.value.json();
          return { method, success: true, data };
        } else {
          return { 
            method, 
            success: false, 
            error: result.status === "rejected" ? result.reason : result.value.statusText 
          };
        }
      })
    );
    
    console.log("Backend test results:", responses);

    return new Response(JSON.stringify({
      success: true,
      testResults: responses,
      summary: {
        getWorking: responses[0].success,
        postWorking: responses[1].success,
        bothWorking: responses[0].success && responses[1].success
      },
      timestamp: new Date().toISOString(),
    }), {
      status: 200,
      headers: {
        "Content-Type": "application/json",
      },
    });

  } catch (error) {
    console.error("Test endpoint error:", error);
    
    return new Response(JSON.stringify({
      success: false,
      error: error instanceof Error ? error.message : "Unknown error",
      timestamp: new Date().toISOString(),
    }), {
      status: 500,
      headers: {
        "Content-Type": "application/json",
      },
    });
  }
};