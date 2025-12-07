package march.dev.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

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
    protected List<String> activeSummaryIds = new ArrayList<>();
    protected Map<String, String> context = new HashMap<>();

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

    public String getLlmResponse(String prompt) {
        return llmClient.generate(prompt);
    }

    public Map<String, String> getContext() {
        return context;
    }

    public void addContext(String key, String value) {
        context.put(key, value);
    }

    public void removeContext(String key) {
        context.remove(key);
    }

    public void clearContext() {
        context.clear();
    }

    public int getHistorySize() {
        return history.length();
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
            if (config != null && history.length() > config.getMaxHistorySize()) {
                handleHistoryOverflow();
            }

            LlmResponse response = null;
            try {
                String prompt = history + ephemeralContext;
                ephemeralContext = "";
                String llmResponseString = getLlmResponse(prompt);
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
                        String content = resultJson.substring(resultJson.indexOf("[EPHEMERAL]") + 11,
                                resultJson.indexOf("[/EPHEMERAL]"));
                        ephemeralContext = "\n[EPHEMERAL_HISTORY_CONTEXT]\n" + content
                                + "\n[END_EPHEMERAL_HISTORY_CONTEXT]\n";
                        systemResponse = new SystemResponse(response.getStep(), response.getToolName(),
                                "History loaded for this turn.", args);
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

                ARCHIVED SUMMARIES: You may see [ARCHIVED_SUMMARY id="..."] markers in your history. These are summaries of past conversations that have been archived to save space. If you need more details about an archived conversation, use 'getHistoryById' with the ID from the marker.
                """;

        systemPrompt = String.format(systemPrompt, this.id);

        if (this.config != null && this.config.getSystemInstruction() != null) {
            systemPrompt += "\n[DEVELOPER_INSTRUCTION]\n";
            for (Map.Entry<String, String> entry : this.config.getSystemInstruction().entrySet()) {
                systemPrompt += "[" + entry.getKey().toUpperCase() + "] : " + entry.getValue() + "\n";
            }
            systemPrompt += "IMPORTANT: The above instructions from [DEVELOPER_INSTRUCTION] apply ONLY to the 'modelAnswer' field in the JSON response. You must still strictly follow the JSON structure defined by [MARCH_FRAMEWORK_INSTRUCTION]. If the developer asks for a specific format (like JSON), that format must be ENCAPSULATED as a string within the 'modelAnswer' field.\n";
        }

        if (!context.isEmpty()) {
            systemPrompt += "\n[USER_CONTEXT]\n";
            for (Map.Entry<String, String> entry : context.entrySet()) {
                systemPrompt += "[" + entry.getKey().toUpperCase() + "]\n"
                        + entry.getValue() + "\n\n";
            }
            systemPrompt += "IMPORTANT: The above context is provided by the developer. Use it to answer user questions accurately.\n";
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
                }\n""";

        systemPrompt += responseFormatPrompt;

        return systemPrompt;
    }

    private void handleHistoryOverflow() {
        int cutIndex = findCutPoint();
        if (cutIndex == -1) {
            return;
        }

        String chunkToArchive = extractChunk(cutIndex);
        if (chunkToArchive.isEmpty()) {
            return;
        }

        // Archive and get summary
        String summaryId = historyService.archive(chunkToArchive);
        String summary = historyService.getSummaryById(summaryId);

        // Add to active summaries
        activeSummaryIds.add(summaryId);

        // Check if we need to drop oldest summary
        if (activeSummaryIds.size() > config.getMaxActiveSummaries()) {
            String droppedId = activeSummaryIds.remove(0);
            removeArchivedSummary(droppedId);
        }

        // Create summary marker
        String summaryMarker = "[ARCHIVED_SUMMARY id=\"" + summaryId + "\"]\n"
                + summary + "\n[/ARCHIVED_SUMMARY]\n\n";

        // Remove the archived chunk from history first
        // Find where to insert summary (after system prompt and existing summaries)
        int lastSummaryEnd = history.lastIndexOf("[/ARCHIVED_SUMMARY]");
        int insertPoint;
        if (lastSummaryEnd != -1) {
            // There are existing summaries, insert after them
            insertPoint = lastSummaryEnd + "[/ARCHIVED_SUMMARY]".length() + 1;
        } else {
            // No summaries yet, insert after system prompt (before first USER_INSTRUCTION)
            insertPoint = history.indexOf("[USER_INSTRUCTION]");
        }

        String beforeChunk = history.substring(0, insertPoint);
        String afterChunk = history.substring(cutIndex);

        // Reconstruct: system prompt + summaries + new summary + remaining history
        history = beforeChunk + summaryMarker + afterChunk;
    }

    private int findCutPoint() {
        int searchStart = history.indexOf("[USER_INSTRUCTION]");
        if (searchStart == -1) {
            return -1;
        }

        int cutIndex = history.indexOf("[USER_INSTRUCTION]", searchStart + 1);
        return cutIndex;
    }

    private String extractChunk(int cutIndex) {
        // Find the first [USER_INSTRUCTION] AFTER all archived summaries
        int lastSummaryEnd = history.lastIndexOf("[/ARCHIVED_SUMMARY]");
        int searchStart = (lastSummaryEnd != -1) ? lastSummaryEnd : 0;

        int firstUserInstruction = history.indexOf("[USER_INSTRUCTION]", searchStart);
        if (firstUserInstruction == -1 || firstUserInstruction >= cutIndex) {
            return "";
        }

        // Extract only from first user instruction to cut point (excludes system prompt
        // and summaries)
        return history.substring(firstUserInstruction, cutIndex);
    }

    private void removeArchivedSummary(String id) {
        String startMarker = "[ARCHIVED_SUMMARY id=\"" + id + "\"]";
        String endMarker = "[/ARCHIVED_SUMMARY]";

        int startIndex = history.indexOf(startMarker);
        if (startIndex != -1) {
            int endIndex = history.indexOf(endMarker, startIndex);
            if (endIndex != -1) {
                history = history.substring(0, startIndex) + history.substring(endIndex + endMarker.length() + 1);
            }
        }
    }
}
