package march.sample;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;

import march.dev.data.Tool;
import march.dev.data.ToolRegistry;
import march.dev.llm.GeminiClient;
import march.dev.llm.OpenRouterClient;
import march.dev.utils.MethodRunner;
import march.dev.utils.ProviderScan;

public class App {
    public static void main(String[] args) {

        try {
            System.out.println("=== March Framework Real-Time Chat ===\n");

            ObjectMapper objectMapper = new ObjectMapper();
            ToolRegistry toolRegistry = new ToolRegistry();
            MethodRunner methodRunner = new MethodRunner();

            // Register tools
            Map<String, Tool> tools = ProviderScan.scanTools("march.sample.services");
            for (Tool tool : tools.values()) {
                toolRegistry.register(tool);
            }

            // Load API keys
            Map<String, String> env = loadEnv();
            String openRouterApiKey = env.get("OPENROUTER_API_KEY");
            String geminiApiKey = env.get("GEMINI_API_KEY");

            if (openRouterApiKey == null || openRouterApiKey.isEmpty() || openRouterApiKey.equals("YOUR_API_KEY_HERE")) {
                openRouterApiKey = System.getenv("OPENROUTER_API_KEY");
            }

            if ((openRouterApiKey == null || openRouterApiKey.isEmpty()) && args.length > 0) {
                openRouterApiKey = args[0];
            }

            if (openRouterApiKey == null) {
                System.out.println("Please provide OPENROUTER_API_KEY environment variable or pass it as an argument.");
                return;
            }

            // Create clients
            OpenRouterClient openRouterClient = new OpenRouterClient(
                openRouterApiKey,
                "nvidia/nemotron-nano-12b-v2-vl:free"
            );

            GeminiClient geminiClient = new GeminiClient(geminiApiKey, "gemini-2.5-flash");

            MyCustomAgent agent = new MyCustomAgent(openRouterClient, toolRegistry, methodRunner, objectMapper);

            // Scanner for real-time input
            Scanner scanner = new Scanner(System.in);
            System.out.println("Type your message and press Enter (type 'exit' to quit):");

            while (true) {
                System.out.print("> ");
                String userInput = scanner.nextLine().trim();

                if (userInput.equalsIgnoreCase("exit")) {
                    System.out.println("Exiting...");
                    break;
                }

                try {
                    // Send user input to agent and get response
                    String response = agent.chat(userInput);
                    System.out.println("\nAI Response:\n" + response + "\n");
                } catch (Exception e) {
                    System.out.println("Error generating response: " + e.getMessage());
                }
            }

            scanner.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static Map<String, String> loadEnv() {
        Map<String, String> env = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(".env"));
            for (String line : lines) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("=", 2);
                if (parts.length == 2) env.put(parts[0].trim(), parts[1].trim());
            }
        } catch (IOException e) {
            System.out.println("No .env file found or error reading it.");
        }
        return env;
    }
}
