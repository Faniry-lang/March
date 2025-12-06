package march.sample;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import march.dev.data.Tool;
import march.dev.data.ToolRegistry;
import march.dev.llm.GeminiClient;
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

            Map<String, String> env = loadEnv();
            String apiKey = env.get("GEMINI_API_KEY");

            if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
                apiKey = System.getenv("GEMINI_API_KEY");
            }

            if ((apiKey == null || apiKey.isEmpty()) && args.length > 0) {
                apiKey = args[0];
            }

            if (apiKey == null) {
                System.out.println("Please provide GEMINI_API_KEY environment variable or pass it as an argument.");
                return;
            }

            GeminiClient geminiClient = new GeminiClient(apiKey);
            MyCustomAgent myCustomAgent = new MyCustomAgent(geminiClient, toolRegistry, methodRunner, objectMapper);
            myCustomAgent.setId("jarvis");

            String userMessage = "Hello, my name is Jean! What's your name?";
            System.out.println("User: " + userMessage);
            String response = myCustomAgent.chat(userMessage);
            System.out.println("Agent: " + response);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    private static Map<String, String> loadEnv() {
        Map<String, String> env = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(".env"));
            for (String line : lines) {
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    env.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException e) {
            System.out.println("No .env file found or error reading it.");
        }
        return env;
    }
}
