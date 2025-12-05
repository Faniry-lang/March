package march.sample;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import march.dev.chat.LlmService;
import march.dev.data.Tool;
import march.dev.data.ToolRegistry;
import march.dev.utils.MethodRunner;
import march.dev.utils.ProviderScan;

public class App {
    public static void main(String[] args) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ToolRegistry toolRegistry = new ToolRegistry();
            MethodRunner methodRunner = new MethodRunner();

            Map<String, Tool> tools = ProviderScan.scanTools("march.sample.services");
            for (Tool tool : tools.values()) {
                toolRegistry.register(tool);
                System.out.println("Registered tool: " + tool.getName());
            }

            String apiKey = System.getenv("GEMINI_API_KEY");
            if (apiKey == null && args.length > 0) {
                apiKey = args[0];
            }

            if (apiKey == null) {
                System.out.println("Please provide GEMINI_API_KEY environment variable or pass it as an argument.");
                return;
            }

            LlmService llmService = new LlmService(toolRegistry, methodRunner, objectMapper, apiKey);

            String userMessage = "Hello";
            System.out.println("User: " + userMessage);
            String response = llmService.chat(userMessage);
            System.out.println("Agent: " + response);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }
}
