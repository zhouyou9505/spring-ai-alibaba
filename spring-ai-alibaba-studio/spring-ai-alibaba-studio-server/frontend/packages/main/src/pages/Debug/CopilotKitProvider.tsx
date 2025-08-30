import { ReactNode } from "react";
import { CopilotKit } from "@copilotkit/react-core";
import "@copilotkit/react-ui/styles.css";

export function CopilotKitProvider({ children }: { children: ReactNode }) {
  return (
    <CopilotKit 
      runtimeUrl="/api/langgraph4j"
    >
      {children}
    </CopilotKit>
  );
}
