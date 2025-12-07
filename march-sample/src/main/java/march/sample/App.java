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
import march.dev.llm.OpenRouterClient;
import march.dev.utils.MethodRunner;
import march.dev.utils.ProviderScan;

public class App {
    public static void main(String[] args) {

        try {
            System.out.println("=== March Framework Test ===\n");

            ObjectMapper objectMapper = new ObjectMapper();
            ToolRegistry toolRegistry = new ToolRegistry();
            MethodRunner methodRunner = new MethodRunner();

            Map<String, Tool> tools = ProviderScan.scanTools("march.sample.services");
            for (Tool tool : tools.values()) {
                toolRegistry.register(tool);
            }

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

            // Create OpenRouter Client with NVIDIA Nemotron model
            OpenRouterClient openRouterClient = new OpenRouterClient(
                openRouterApiKey,
                "nvidia/nemotron-nano-12b-v2-vl:free"
            );

            GeminiClient geminiClient = new GeminiClient(geminiApiKey, "gemini-2.5-flash");

            MyCustomAgent agent = new MyCustomAgent(openRouterClient, toolRegistry, methodRunner, objectMapper);

            // Test 1: Context Management
            System.out.println("TEST 1: Context Management");
            System.out.println("---------------------------");
            agent.addContext("user_info", "Name: Alice, Age: 30, Occupation: Software Engineer");

            String response1 = agent.chat("What's my name?");
            System.out.println("Q: What's my name?");
            System.out.println("A: " + response1);
            System.out.println("History size: " + agent.getHistorySize() + " characters\n");

            // Test 2: Multiple conversations to trigger rolling window
            System.out.println("TEST 2: Filling History (to trigger rolling window)");
            System.out.println("-----------------------------------------------------");

            String[] questions = {
                    "What's 2+2?",
                    "Say hello in French",
                    "What color is the sky?"
            };

            for (int i = 0; i < questions.length; i++) {
                System.out.println("Q" + (i + 1) + ": " + questions[i]);
                String response = agent.chat(questions[i]);
                System.out.println("A" + (i + 1) + ": " + response);
                System.out.println("History size: " + agent.getHistorySize() + " characters");

                if (agent.getHistorySize() > 2000) {
                    System.out.println("⚠️  History exceeded maxHistorySize (2000)!");
                }
                System.out.println();

                // Small delay to avoid rate limits
                Thread.sleep(3000);
            }

            // Test 3: Check if archiving occurred
            System.out.println("TEST 3: Verify Archiving");
            System.out.println("-------------------------");
            checkArchiveFiles();
            System.out.println();

            // Test 4: Context still works after rolling window
            System.out.println("TEST 4: Context Persistence After Rolling Window");
            System.out.println("--------------------------------------------------");
            String response2 = agent.chat("What's my occupation?");
            System.out.println("Q: What's my occupation? (context should still be available)");
            System.out.println("A: " + response2);
            System.out.println();

            System.out.println("=== All Tests Complete ===");
            System.out.println("\nSummary:");
            System.out.println("- Final history size: " + agent.getHistorySize() + " characters");
            System.out.println("- Context entries: " + agent.getContext().size());
            System.out.println("- Check archive files above for rolling window results");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void checkArchiveFiles() {
        String archivePath = "src/main/resources/march-history/jarvis/";
        try {
            if (Files.exists(Paths.get(archivePath + "index.json"))) {
                String indexContent = Files.readString(Paths.get(archivePath + "index.json"));
                System.out.println("Archive index.json content:");
                System.out.println(indexContent);

                // Count archived entries
                int count = indexContent.split("\"id\"").length - 1;
                System.out.println("\nNumber of archived history chunks: " + count);

                if (count > 0) {
                    System.out.println("✅ Rolling window archiving is working!");
                    System.out.println("   History was cut and archived when it exceeded maxHistorySize");
                } else {
                    System.out.println("ℹ️  No archives created yet");
                    System.out.println("   This is normal if history hasn't exceeded maxHistorySize (2000 chars)");
                    System.out.println("   Try running more conversations to trigger archiving");
                }
            } else {
                System.out.println("ℹ️  No archive directory found yet");
            }
        } catch (IOException e) {
            System.out.println("Could not read archive files: " + e.getMessage());
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
