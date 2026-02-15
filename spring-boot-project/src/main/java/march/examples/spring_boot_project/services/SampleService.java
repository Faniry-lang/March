package march.examples.spring_boot_project.services;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;

import march.annotations.MarchProvider;
import march.annotations.MarchTool;
import march.enums.ToolType;
import march.agent.Agent;
import march.agent.OpenRouterClient;
import march.tools.ToolRegistry;
import march.examples.spring_boot_project.agents.DebtAgent;

@Service
@MarchProvider
public class SampleService {

    @Autowired
    private ToolRegistry toolRegistry;
    
    @Autowired
    private ObjectMapper objectMapper;

    @MarchTool(name = "greet", description = "Greets a user", type = ToolType.FUNCTION)
    public String greet(String name) {
        return "Hello, " + name + "!";
    }

    public String simulateDebtCalculation(String apiKey, String principal, String rate, String time) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("API Key cannot be null or empty");
        }

        Agent debtAgent = new DebtAgent(apiKey, objectMapper, toolRegistry, new OpenRouterClient());
        
        String query = String.format("Calculate the total debt for a principal of %s with a simple interest rate of %s for %s years.", principal, rate, time);
        
        String chatId = "debt-simulation-" + System.currentTimeMillis();
        debtAgent.createNewChat(chatId);
        
        return debtAgent.chat(query, chatId, "google/gemini-2.0-flash-001");
    }

    public String whatsYourName(String apiKey) throws Exception {
        Agent debtAgent = new DebtAgent(apiKey, objectMapper, toolRegistry, new OpenRouterClient());
        String chatId = "name-query-" + System.currentTimeMillis();
        debtAgent.createNewChat(chatId);
        return debtAgent.chat("What's your name, agent?", chatId, "google/gemini-2.0-flash-001");
    }
}
