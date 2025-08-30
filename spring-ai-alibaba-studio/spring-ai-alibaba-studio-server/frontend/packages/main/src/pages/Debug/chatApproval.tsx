import { useCopilotAction } from "@copilotkit/react-core";
import { CopilotChat } from "@copilotkit/react-ui";

export function SimpleChatWithApproval() {
  const copilotAction = useCopilotAction({
    name: "test-action",
    description: "A test action that requires approval",
    argumentAnnotations: [
      {
        name: "message",
        type: "string",
        description: "The message to send",
        required: true,
      },
    ],
    handler: async ({ message }) => {
      return `Action executed with message: ${message}`;
    },
  });

  return (
    <CopilotChat
      instructions={"You are assisting the user as best as you can. Answer in the best way possible given the data you have."}
      labels={{
        title: "Your Assistant",
        initial: "Hi! 👋 How can I assist you today?",
      }}
      actions={[copilotAction]}
    />
  );
}
