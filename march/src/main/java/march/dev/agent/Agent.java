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
import java.util.LinkedHashMap;
import march.dev.history.HistoryService;
import march.dev.llm.LlmClientFactory;

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
        // create a per-agent MethodRunner (namespaced cache keys)
        this.methodRunner = new MethodRunner();
        this.objectMapper = objectMapper;

        if (this.getClass().isAnnotationPresent(AgentId.class)) {
            AgentId agentAnnotation = this.getClass().getAnnotation(AgentId.class);
            this.id = agentAnnotation.value();
            try {
                this.config = ConfigLoader.loadConfig(this.id);

                // If no llmClient was provided, try to construct one from config
                if (this.llmClient == null) {
                    System.out.println("LlmClient not provided, attempting to create from agent config...");
                    try {
                        LlmClient created = LlmClientFactory.createFromConfig(this.config);
                        if (created != null) this.llmClient = created;
                    } catch (Exception e) {
                        System.out.println("Could not create LlmClient from config: " + e.getMessage());
                    }
                }

                // create history service (may require llmClient for summarization)
                this.historyService = new HistoryService(this.id, this.llmClient, this.objectMapper);

                // Create a per-agent ToolResultCache and MethodRunner using config TTL
                try {
                    long ttl = 5 * 60 * 1000;
                    if (this.config != null && this.config.getCacheTtlMs() > 0) ttl = this.config.getCacheTtlMs();
                    march.dev.utils.ToolResultCache agentCache = new march.dev.utils.ToolResultCache(ttl);
                    this.methodRunner = new MethodRunner(this.id, agentCache);
                } catch (Exception e) {
                    // fallback: keep existing methodRunner
                }

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
        if (this.chatSession == null) {
            String sessionId = UUID.randomUUID().toString();
            this.chatSession = new ChatSession(
                sessionId, this, this.objectMapper, this.methodRunner, this.toolRegistry
            );
        }
    }

    public void closeChatSession() {
        try {
            if (this.historyService != null) {
                this.historyService.deleteTransientHistory();
            } else {
                Files.deleteIfExists(Paths.get("src/main/resources/history.json"));
            }
        } catch (Exception e) {
            System.out.println("Unable to delete history.json: " + e.getMessage());
        }
        this.chatSession = null;
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
                String prompt = this.chatSession.getHistory();
                response = this.chatSession.getLlmResponse(prompt);

                if (response.isFunctionCall()) {
                    this.chatSession.executeOrder(response);
                    continue;
                }

                try {
                    String historyJson = objectMapper.writeValueAsString(java.util.Map.of("history", this.chatSession.getHistory()));
                    if (this.historyService != null) {
                        this.historyService.writeTransientHistory(historyJson);
                    } else {
                        Files.writeString(Paths.get("src/main/resources/history.json"), historyJson);
                    }
                } catch (Exception ex) {
                    System.out.println("Unable to write history json: " + ex.getMessage());
                }
                this.chatSession.endUserRequest();
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
        try {
            LinkedHashMap<String, Object> root = new LinkedHashMap<>();
            root.put("agentId", this.id);

            Object toolsForAgent = toolRegistry.getToolsForAgent(this.id);
            root.put("tools", toolsForAgent != null ? toolsForAgent : new LinkedHashMap<>());

            if (this.config != null && this.config.getSystemInstruction() != null) {
                root.put("developerInstruction", this.config.getSystemInstruction());
            }

            if (!this.context.isEmpty()) {
                root.put("userContext", this.context);
            }

            java.util.Map<String, Object> marchFramework = new java.util.LinkedHashMap<>();

            java.util.Map<String, Object> schema = new java.util.LinkedHashMap<>();
            schema.put("type", "object");
            java.util.Map<String, Object> props = new java.util.LinkedHashMap<>();
            props.put("step", java.util.Map.of("type", "integer"));
            props.put("modelThought", java.util.Map.of("type", "string"));
            props.put("modelAnswer", java.util.Map.of("type", "string"));
            props.put("functionCall", java.util.Map.of("type", "boolean"));
            props.put("toolName", java.util.Map.of("type", "string", "nullable", true));
            props.put("arguments", java.util.Map.of("type", "object", "nullable", true));
            schema.put("properties", props);
            schema.put("required", java.util.List.of("step", "modelThought", "modelAnswer", "functionCall"));

            marchFramework.put("responseSchema", schema);

            java.util.Map<String, Object> example = new java.util.LinkedHashMap<>();
            example.put("step", 1);
            example.put("modelThought", "I should check tools before answering.");
            example.put("modelAnswer", "Here is a concise answer to the user request.");
            example.put("functionCall", false);

            marchFramework.put("exampleResponse", example);

            marchFramework.put("note", "Step is the current step of the user request process, modelThought is YOUR thought process, modelAnswer is YOUR answer for the user request. Return ONLY a single JSON object matching 'responseSchema'. Do not include any markdown, explanation, or extra text.");

            root.put("marchFramework", marchFramework);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new Exception("Error building compact system prompt: " + e.getMessage(), e);
        }
    }

    public march.dev.config.AgentConfig getConfig() {
        return this.config;
    }
}
