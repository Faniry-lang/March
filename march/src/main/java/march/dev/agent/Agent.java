package march.dev.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import march.dev.config.AgentConfig;
import march.dev.config.ConfigLoader;
import march.dev.data.ToolRegistry;
import march.dev.llm.LlmClient;
import march.dev.process.LlmResponse;
import march.dev.annotations.AgentId;
import march.dev.chat.ChatSession;
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
    protected ChatSession chatSession;

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

    public void openChatSession() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        this.chatSession = new ChatSession(
            sessionId, this, this.objectMapper, this.methodRunner, this.toolRegistry
        );
    }

    public void startChainOfThought(String userRequest) throws Exception {
        this.chatSession.initResponseChain();
        this.chatSession.setUserRequest(userRequest);
    }

    public String chat(String userMessage) throws Exception {

        if(this.chatSession == null) {
            this.openChatSession();
        }

        this.startChainOfThought(userMessage);

        int errorCount = 0;

        while (true) {
            LlmResponse response = null;
            try {
                String prompt = history + ephemeralContext;
                ephemeralContext = "";

                response = this.chatSession.getLlmResponse(prompt);

                if (response.isFunctionCall()) {
                    this.chatSession.executeOrder(response);
                    continue;
                }

                Files.writeString(Paths.get("src/main/resources/history.xml"), this.chatSession.getHistory());
                return response.getModelAnswer();

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Erreur: " + e.getMessage());
                response = new LlmResponse(-1, "Une erreur s'est produite " + e.getMessage(), "", false, "", null);
                this.chatSession.writeResponseInHistory(response);

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
}
