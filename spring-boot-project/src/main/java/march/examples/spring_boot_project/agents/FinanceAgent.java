package march.examples.spring_boot_project.agents;

import march.agent.Agent;
import march.agent.LlmClient;
import march.annotations.MarchAgent;
import march.tools.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

@MarchAgent(name = "FinanceAdvisor")
public class FinanceAgent extends Agent {
    
    public FinanceAgent(String apiKey, ObjectMapper objectMapper, ToolRegistry toolRegistry, LlmClient llmClient) {
        super( 16000, apiKey, objectMapper, toolRegistry, llmClient);
        this.setSystemPrompt("""
            You are an expert Financial Advisor and Analyst. 
            You have access to a suite of precise financial calculation tools.
            
            Your goal is to help users with complex financial decision making, planning, and analysis.
            
            Guidelines:
            1. ALWAYS use the provided tools for calculations. Do not rely on your internal training data for math.
            2. If a user asks a complex question (e.g., "Should I invest in A or B?"), break it down into steps:
               - Calculate metrics for Option A (ROI, NPV, etc.)
               - Calculate metrics for Option B
               - Compare and provide a recommendation based on the data.
            3. Be transparent about assumptions (e.g., inflation rates used if not specified).
            4. Format your final answers clearly, using bullet points for key figures.
            5. If you lack information (like tax rate or time period), ask the user for clarification before proceeding, or state your assumption clearly.
            
            You are professional, accurate, and helpful.
        """);
    }

    @Override
    public CostPerPrompt onAgentUsage(DetailedPromptInfo info) {
        CostPerPrompt cost = new CostPerPrompt();
        
        // Gemini 2.0 Flash pricing estimate
        // Input: $0.10 / 1M tokens (~4M chars)
        // Output: $0.40 / 1M tokens (~4M chars)
        double inputCharsPerMillion = 4_000_000.0;
        double outputCharsPerMillion = 4_000_000.0;
        
        double apiCostInput = (info.getInputTokenLength() / inputCharsPerMillion) * 0.10;
        double apiCostOutput = (info.getOutPutTokenLength() / outputCharsPerMillion) * 0.40;
        
        cost.setApiCost(apiCostInput + apiCostOutput);
        
        // Compute cost: $0.01 per second (as an example for premium advisor compute)
        double computeCost = (info.getProcessingTime() / 1000.0) * 0.01;
        cost.setComputeCost(computeCost);
        
        return cost;
    }

    @Override
    public march.agent.CostMetrics getCostMetrics() {
        return super.getCostMetrics();
    }
}
