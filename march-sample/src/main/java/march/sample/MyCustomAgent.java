package march.sample;

import com.fasterxml.jackson.databind.ObjectMapper;

import march.dev.agent.Agent;
import march.dev.data.ToolRegistry;
import march.dev.llm.LlmClient;
import march.dev.utils.MethodRunner;

public class MyCustomAgent extends Agent {

    public MyCustomAgent(LlmClient llmClient, ToolRegistry toolRegistry, MethodRunner methodRunner,
            ObjectMapper objectMapper) {
        super(llmClient, toolRegistry, methodRunner, objectMapper);
    }

    @Override
    public String getSystemInstruction() {
        return """
                You are a helpful assistant named Jarvis.
                You are very polite and formal.
                """;
    }

    @Override
    public String getModel() {
        return "gemini-2.5-flash";
    }
}
