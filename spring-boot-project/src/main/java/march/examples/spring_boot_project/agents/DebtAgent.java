package march.examples.spring_boot_project.agents;

import march.agent.Agent;
import march.agent.LlmClient;
import march.annotations.MarchAgent;
import march.tools.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

@MarchAgent(name = "DebtCalculator")
public class DebtAgent extends Agent {
    
    public DebtAgent(String apiKey, ObjectMapper objectMapper, ToolRegistry toolRegistry, LlmClient llmClient) {
        super(  8000, apiKey, objectMapper, toolRegistry, llmClient);
        this.setSystemPrompt("""
            You are a helpful assistant specialized in calculating debt.
            You have access to tools that can calculate interest and total debt.
            Please use these tools to answer debt-related questions.
            Always provide the final calculated amount clearly.
        """);
    }

    @Override
    public CostPerPrompt onAgentUsage(DetailedPromptInfo info) {
        CostPerPrompt cost = new CostPerPrompt();
        
        // Gemini 2.0 Flash - $0.10 per 1M tokens (input), $0.40 per 1M tokens (output)
        // Estimating 1 token = 4 characters
        double inputCharsPerMillion = 4_000_000.0;
        double outputCharsPerMillion = 4_000_000.0;
        
        double apiCostInput = (info.getInputTokenLength() / inputCharsPerMillion) * 0.10;
        double apiCostOutput = (info.getOutPutTokenLength() / outputCharsPerMillion) * 0.40;
        
        cost.setApiCost(apiCostInput + apiCostOutput);
        
        // Sample compute cost: $0.005 per second of processing
        double computeCost = (info.getProcessingTime() / 1000.0) * 0.005;
        cost.setComputeCost(computeCost);
        
        return cost;
    }
}
