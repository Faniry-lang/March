package march.sample;

import com.fasterxml.jackson.databind.ObjectMapper;

import march.dev.agent.Agent;
import march.dev.chat.LlmService;
import march.dev.data.ToolRegistry;
import march.dev.utils.MethodRunner;

public class MyCustomAgent extends Agent {

    public MyCustomAgent(LlmService llmService, ToolRegistry toolRegistry, MethodRunner methodRunner,
            ObjectMapper objectMapper) {
        super(llmService, toolRegistry, methodRunner, objectMapper);
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
