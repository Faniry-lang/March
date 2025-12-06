package march.sample;

import com.fasterxml.jackson.databind.ObjectMapper;

import march.dev.agent.Agent;
import march.dev.data.ToolRegistry;
import march.dev.llm.LlmClient;
import march.dev.utils.MethodRunner;
import march.dev.annotations.AgentId;

@AgentId("jarvis")
public class MyCustomAgent extends Agent {

    public MyCustomAgent(LlmClient llmClient, ToolRegistry toolRegistry, MethodRunner methodRunner,
            ObjectMapper objectMapper) {
        super(llmClient, toolRegistry, methodRunner, objectMapper);
    }
}
