package march.dev.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

import march.dev.config.AgentConfig;
import march.dev.config.ConfigLoader;
import march.dev.data.Tool;
import march.dev.data.ToolRegistry;
import march.dev.llm.LlmClient;
import march.dev.process.LlmResponse;
import march.dev.process.SystemResponse;
import march.dev.utils.JsonUtils;
import march.dev.annotations.AgentId;
import march.dev.utils.MethodRunner;
import march.dev.history.HistoryService;

public abstract class Agent {

    protected LlmClient llmClient;
    protected ToolRegistry toolRegistry;
    protected MethodRunner methodRunner;
    protected ObjectMapper objectMapper;
    protected String history = "";
    protected String id;
    protected AgentConfig config;
    protected HistoryService historyService;
    protected String ephemeralContext = "";

    public Agent(LlmClient llmClient, ToolRegistry toolRegistry, MethodRunner methodRunner,
            ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.methodRunner = methodRunner;
        this.objectMapper = objectMapper;

        if (this.getClass().isAnnotationPresent(AgentId.class)) {
            AgentId agentAnnotation = this.getClass().getAnnotation(AgentId.class);
            this.id = agentAnnotation.value();
            try {
                this.config = ConfigLoader.loadConfig(this.id);
                this.historyService = new HistoryService(this.id, this.llmClient, this.objectMapper);
            } catch (Exception e) {
                System.out.println("Could not load config for agent " + this.id + ": " + e.getMessage());
            }
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModel() {
        if (config != null && config.getModel() != null) {
            return config.getModel();
        }
        return null;
    }

    public String getLlmResponse(String prompt, String modelName) {
        return llmClient.generate(prompt, modelName);
    }

    public String chat(String userMessage) throws Exception {

        if (!history.contains("[MARCH_FRAMEWORK_INSTRUCTION]")) {
            history += this.getSystemPrompt();
        }
        history += "[USER_INSTRUCTION]\n" + userMessage;
        history += " [CHAIN_OF_THOUGHT]\n";
        history += " [START]... \n";

        int errorCount = 0;

        while (true) {
            if (history.length() > config.getMaxHistorySize()) {
                int cutIndex = history.indexOf("[USER_INSTRUCTION]", history.length() / 2);
                if (cutIndex != -1) {
                    String toArchive = history.substring(0, cutIndex);
                    String toKeep = history.substring(cutIndex);

                    if (toArchive.contains("[MARCH_FRAMEWORK_INSTRUCTION]")) {
                         int systemPromptEnd = history.indexOf("[USER_INSTRUCTION]");
                         if (systemPromptEnd != -1 && cutIndex > systemPromptEnd) {
                             toArchive = history.substring(systemPromptEnd, cutIndex);
                             toKeep = history.substring(0, systemPromptEnd) + history.substring(cutIndex);
                         }
                    }
                    
                    historyService.archive(toArchive);
                    history = toKeep;
                }
            }

            LlmResponse response = null;
            try {
                String prompt = history + ephemeralContext;
                ephemeralContext = ""; 
                
                String llmResponseString = getLlmResponse(prompt, getModel());
                String cleanedLlmResponseString = JsonUtils.cleanLlmResponse(llmResponseString);
                history += " [MODEL_RESPONSE]\n" + cleanedLlmResponseString + "\n";
                response = objectMapper.readValue(cleanedLlmResponseString, LlmResponse.class);

                if (response.isFunctionCall()) {
                    Tool tool = toolRegistry.get(response.getToolName());
                    Object[] args = response.getOrderedAndTypedArgs(toolRegistry, objectMapper);
                    Object result = methodRunner.execute(tool, args);
                    String resultJson = objectMapper.writeValueAsString(result);

                    SystemResponse systemResponse = new SystemResponse(response.getStep(), response.getToolName(),
                            resultJson, args);
                    String systemResponseJson = objectMapper.writeValueAsString(systemResponse);
                    
                    if (resultJson.contains("[EPHEMERAL]")) {
                        String content = resultJson.substring(resultJson.indexOf("[EPHEMERAL]") + 11, resultJson.indexOf("[/EPHEMERAL]"));
                        ephemeralContext = "\n[EPHEMERAL_HISTORY_CONTEXT]\n" + content + "\n[END_EPHEMERAL_HISTORY_CONTEXT]\n";
                        systemResponse = new SystemResponse(response.getStep(), response.getToolName(), "History loaded for this turn.", args);
                        systemResponseJson = objectMapper.writeValueAsString(systemResponse);
                    }
                    
                    history += " [BACKEND_RESPONSE]\n" + systemResponseJson + "\n";
                    continue;
                }

                history += " ...[END]\n";
                return response.getModelAnswer();

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Erreur: " + e.getMessage());
                response = new LlmResponse(-1, "Une erreur s'est produite " + e.getMessage(), "", false, "", null);
                history += " [BACKEND_RESPONSE]\n[ERROR]\n" + objectMapper.writeValueAsString(response) + "\n";
                errorCount++;

                if (errorCount > 3) {
                    return "Erreur lors de la génération de la réponse...";
                }

                continue;
            }
        }
    }

    public String getSystemPrompt() throws Exception {
        String toolJson = "";

        try {
            toolJson = objectMapper.writeValueAsString(toolRegistry.getToolsForAgent(this.id));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new Exception("Error during tool serialization: " + e.getMessage());
        }

        String systemPrompt = """
                [MARCH_FRAMEWORK_I

                AI Agents in Java.

                HIERARCHY OF INSTRUCTIONS:
                1. [MARCH_FRAMEWORK_INSTRUCTION]: Highest priority. You MUST follow these instructions for JSON structure, tool usage, and behavior. NEVER contra

                3. [USER_INSTRUCTION]: Lowest priority. This is the user's query. You should answer it while respecting the constraints of the higher priorities.

                Your goal is to answer any question from the user while strictly adhering to this hierarchy.
                
                Your Agent ID is: %s. You can use this ID to access your history tools.
                You have access to your past history. If you need to recall something, use 'getHistorySummaries' with your ID to find relevant chunks, then 'getHistoryById' to load them.
                """;
        
        systemPrompt = String.format(systemPrompt, this.id);

        if (this.config != null && this.config.getSystemInstruction() != null) {
            systemPrompt += "\n[DEVELOPER_INSTRUCTION]\n";
            for (Map.Entry<String, String> entry : this.config.getSystemInstruction().entrySet()) {
                systemPrompt += "[" + entry.getKey().toUpperCase() + "] : " + entry.getValue() + "\n";
            }
            systemPrompt += "IMPORTANT: The above instructions from [DEVELOPER_INSTRUCTION] apply ONLY to the 'modelAnswer' field in the JSON response. You must still strictly follow the JSON structure defined by [MARCH_FRAMEWORK_INSTRUCTION]. If the developer asks for a specific format (like JSON), that format must be ENCAPSULATED as a string within the 'modelAnswer' field.\n";
        }

        systemPrompt += "\n[MARCH_FRAMEWORK_INSTRUCTION] (Tools List)\n" + toolJson + "\n";

        String responseFormatPrompt = """
                [SYSTEM_RESPONSE_FORMAT]
                All of your answers must follow this structure:
                {
                    "step": ,
                    "modelThought": ,
                    "modelAnswer": ,
                    "functionCall": ,
                    "toolName": ,
                    "arguments": {}
                }

                You must return nothing else except a JSON object with this
                structure.
                Do NOT return any Markdown code block delimiters, NO backticks (```), and NO comments.
                Your response must start directly with the opening brace '{' and end with the closing brace '}'.
                The "step" attribute is the step number of the request, from 1 to n.
                All comments you make must be in "modelThought";
                the answer for the user will 

                if the user's request does not require a function call, this will always be false.
                The "toolName" attribute is the name of the tool you have chosen to answer the user's request.
                IMPORTANT: If no tool can satisfy the request, do not try to create answers
                from scratch; just state that you do not have the necessary tools to execute the request.
                "toolName" MUST be a tool name from the list cited earlier (I mean the "name" attribute); you must NOT invent one.
                Finally, the "arguments" attribute is an array of arguments for the tool you have chosen. The arguments
                come from previous function calls you have made or directly from the user's message. They must be in the same
                order as the "params" attribute of the tool object.

                [SYSTEM_RESPONSE_EXAMPLE]
                Example: User asks "How many leave days does Jean have?"

                Step 1: Find Jean's ID.
                {
                    "step": 1,
                    "modelThought": "The user asks for Jean's leave days. I need 'employeId' to call 'getCongeByEmployeId'. I don't have it, so I will first search for Jean's ID using 'findEmployeByName'.",
                    "modelAnswer": "",
                    "functionCall": true,
                    "toolName": "findEmployeByName",
                    "arguments": {
                        "name": "Jean"
                    }
                }

                Step 2: Use the ID to get leave days.
                {
                    "step": 2,
                    "modelThought": "I have found that Jean's ID is 123. Now I can call 'getCongeByEmployeId' with this ID.",
                    "modelAnswer": "",

                    "toolName": "getCongeByEmployeId",
                    "arguments": {
                        "employeId": 123
                    }
                }

                [SYSTEM_TOOL_ARGUMENTS_NAME]
                It is very IMPORTANT that the names of the arguments you provide in your response are the same
                as those listed in the "params" attribute of the Tool. If you are unsure about their names, you can
                call tools related to the Tool like "getToolParams" to be sure of their names.\n

                    """;

        systemPrompt += responseFormatPrompt;        

        return systemPrompt;
    }
}
