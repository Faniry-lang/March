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
// Removed Spring AI adapter import; using existing LLM clients instead
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
                "nvidia/nemotron-nano-12b-v2-vl:free",
                null
            );

            GeminiClient geminiClient = new GeminiClient(geminiApiKey, "gemini-2.5-pro");

            // Let Agent create its own LlmClient from agent config (per-agent provider)
            // Pass `null` for llmClient so Agent uses LlmClientFactory with the jarvis.json settings
            MyCustomAgent agent = new MyCustomAgent(null, toolRegistry, methodRunner, objectMapper);

            // Replace interactive mode with batch test requests to simulate production workload
            String[] testRequests = new String[] {
                "Compute the first 12 terms of the Fibonacci sequence and provide a short explanation of how you computed them.",
                "Given portfolio: {AAPL: 50, MSFT: 20, TSLA: 5}, simulate a 1-year monthly return sequence with random noise and report final portfolio value and a short CSV of monthly values.",
                "Summarize the following long document into 5 bullet points: Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed non risus... (repeat to make it long)",
                "Perform a detailed statistical analysis (mean, median, stdev, 95% CI) for the list: 12, 15, 14, 19, 11, 22, 18, 17.",
                "Translate the sentence 'Bonjour, comment allez-vous aujourd'hui?' to English and provide an alternate more formal phrasing.",
                "Refactor this Java method for clarity and performance: public int[] reverse(int[] a){ int n=a.length; for(int i=0;i<n/2;i++){int t=a[i];a[i]=a[n-i-1];a[n-i-1]=t;} return a;}",
                "Extract entities and dates from: 'Schedule a meeting with Dr. Smith on March 15th, 2026 at 3pm, and send follow up.'",
                "Given a CSV content with columns (name,age,salary) with 1000 rows (simulate), compute median age and average salary per decade bucket and return as JSON.",
                "Generate a 300-word technical report about scalable vector search architectures, include headers and a short conclusion.",
                "Compute the shortest route visiting these coordinates (approx): (48.8566,2.3522),(51.5074,-0.1278),(40.7128,-74.0060) and explain the reasoning.",
                "Given text: 'The battery lasts 10h under light use', output sentiment and a 2-sentence product blurb targeted at engineers.",
                "Run a simulated heavy numeric task: multiply two 10x10 matrices filled with 1..100 and return the resulting matrix in CSV format."
            };

            for (String request : testRequests) {
                System.out.println("\n=== User Request ===\n" + request + "\n");
                try {
                    String response = agent.chat(request);
                    System.out.println("=== AI Response ===\n" + response + "\n");
                } catch (Exception e) {
                    System.out.println("Error generating response for request: " + e.getMessage());
                    agent.closeChatSession();
                    // Recreate agent using the same OpenRouter client for next request if available
                    agent = new MyCustomAgent(openRouterClient, toolRegistry, methodRunner, objectMapper);
                }
            }
            agent.closeChatSession();
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
