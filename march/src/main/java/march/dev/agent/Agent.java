package march.dev.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
import march.dev.utils.ResourceUtils;
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
            // if (config != null && history.length() > config.getMaxHistorySize()) {
            //     handleHistoryOverflow();
            // }

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

        String systemPromptTemplate = ResourceUtils.readResourceFile("march-agent-system-prompt.xml");

        String prompt = systemPromptTemplate.replace("[PLACEHOLDER: toolJson goes here, detailing tool names, functions, and arguments.]", toolJson);

        StringBuilder additionalInstructions = new StringBuilder();
        additionalInstructions.append("<agent-info>");
        additionalInstructions.append("    <agent-id>").append(this.id).append("</agent-id>");
        additionalInstructions.append("    <archived-summaries-instruction>Use [ARCHIVED_SUMMARY id=\"...\"] to refer to past summaries. Load details via 'getHistoryById'.</archived-summaries-instruction>");
        additionalInstructions.append("</agent-info>");

        StringBuilder developerInstruction = new StringBuilder();
        if (this.config != null && this.config.getSystemInstruction() != null) {
            developerInstruction.append("<developer-instruction>");
            this.config.getSystemInstruction().forEach((k, v) -> developerInstruction.append("    <").append(k.toLowerCase()).append(">").append(v).append("</").append(k.toLowerCase()).append(">"));
            developerInstruction.append("    <important>These instructions apply only to 'modelAnswer'. JSON structure from [MARCH_FRAMEWORK_INSTRUCTION] must always be followed.</important>");
            developerInstruction.append("</developer-instruction>");
        }

        StringBuilder userContext = new StringBuilder();
        if (!context.isEmpty()) {
            userContext.append("<user-context>");
            context.forEach((k, v) -> userContext.append("    <").append(k.toLowerCase()).append(">").append(v).append("</").append(k.toLowerCase()).append(">"));
            userContext.append("    <important>The above context is provided by the developer. Use it to answer user questions accurately.</important>");
            userContext.append("</user-context>");
        }
        
        prompt = prompt.replace("[PLACEHOLDER: agent-info]", additionalInstructions.toString());
        prompt = prompt.replace("[PLACEHOLDER: developer-instruction]", developerInstruction.toString());
        prompt = prompt.replace("[PLACEHOLDER: user-context]", userContext.toString());

        return prompt;
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
