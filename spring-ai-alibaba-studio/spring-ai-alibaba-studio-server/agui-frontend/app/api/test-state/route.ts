import { NextRequest } from "next/server";

/**
 * Test endpoint to check the backend /state response directly
 */
export const GET = async (req: NextRequest) => {
  try {
    const backendUrl = process.env.BACKEND_URL || "http://localhost:8080";
    
    console.log("Testing backend /state endpoint directly...");
    
    // Test data similar to what CopilotKit would send
    const testRequests = [
      {
        name: "Valid Request",
        body: {
          threadId: "test-thread-123",
          runId: "test-run-456",
          name: "ai_researcher"
        }
      },
      {
        name: "Missing ThreadId",
        body: {
          runId: "test-run-789",
          name: "ai_researcher"
        }
      },
      {
        name: "Empty ThreadId",
        body: {
          threadId: "",
          runId: "test-run-999",
          name: "ai_researcher"
        }
      },
      {
        name: "Null ThreadId",
        body: {
          threadId: null,
          runId: "test-run-000",
          name: "ai_researcher"
        }
      }
    ];
    
    const results = await Promise.allSettled(
      testRequests.map(async (testCase) => {
        const response = await fetch(`${backendUrl}/copilotkit/state`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(testCase.body),
        });

        if (!response.ok) {
          throw new Error(`Backend responded with status ${response.status}`);
        }

        const data = await response.json();
        
        return {
          testCase: testCase.name,
          success: true,
          data,
          validation: {
            hasThreadId: 'threadId' in data,
            threadIdNotNull: data.threadId != null,
            threadIdNotEmpty: data.threadId != null && data.threadId.trim() !== '',
            hasRequiredFields: ['threadId', 'status', 'actions', 'messages', 'tools'].every(field => field in data),
            actionsIsArray: Array.isArray(data.actions),
            messagesIsArray: Array.isArray(data.messages),
            toolsIsArray: Array.isArray(data.tools)
          }
        };
      })
    );
    
    const testResults = results.map((result, index) => {
      if (result.status === "fulfilled") {
        return result.value;
      } else {
        return {
          testCase: testRequests[index].name,
          success: false,
          error: result.reason?.message || "Unknown error"
        };
      }
    });

    console.log("Backend /state test results:", testResults);

    return new Response(JSON.stringify({
      success: true,
      testResults,
      summary: {
        totalTests: testResults.length,
        passed: testResults.filter(r => r.success).length,
        failed: testResults.filter(r => !r.success).length,
        allValid: testResults.every(r => r.success && (r as any).validation?.threadIdNotNull)
      },
      timestamp: new Date().toISOString(),
    }), {
      status: 200,
      headers: {
        "Content-Type": "application/json",
      },
    });

  } catch (error) {
    console.error("Test /state endpoint error:", error);
    
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