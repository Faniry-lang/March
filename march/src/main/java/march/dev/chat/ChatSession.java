package march.dev.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import march.dev.agent.Agent;
import march.dev.data.Tool;
import march.dev.data.ToolRegistry;
import march.dev.process.LlmResponse;
import march.dev.process.Message;
import march.dev.process.Response;
import march.dev.process.SystemResponse;
import march.dev.utils.JsonUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import march.dev.utils.MethodRunner;

public class ChatSession {
    
    String sessionId;
    Agent agent;
    List<Message> historyMessages;
    ObjectMapper objectMapper;
    MethodRunner methodRunner;
    ToolRegistry toolRegistry;
    List<Response> responseChain;

    public ChatSession(String sessionId, Agent agent, ObjectMapper objectMapper,
            MethodRunner methodRunner, ToolRegistry toolRegistry) throws Exception {
        this.sessionId = sessionId;
        this.agent = agent;
        this.objectMapper = objectMapper;
        this.methodRunner = methodRunner;
        this.toolRegistry = toolRegistry;
        this.responseChain = new ArrayList<>();
        this.initHistory();
    }

    public void initHistory() throws Exception {
        this.historyMessages = new ArrayList<>();
        String systemPrompt = this.agent.getSystemPrompt();
        Message systemMessage = new Message("system", systemPrompt, null, null);
        this.historyMessages.add(systemMessage);
    }

    public void setUserRequest(String userRequest) {
        Message userMessage = new Message("user", userRequest, null, null);
        this.historyMessages.add(userMessage);
    }

    public void endUserRequest() {
        // This method is obsolete
    }

    public void initResponseChain() {
        this.responseChain = new ArrayList<>();
    }

    public void writeResponseInHistory(Response response) throws Exception {
        String respJson = response.toJson();
        Message assistantMessage = new Message("assistant", respJson, null, null);
        this.historyMessages.add(assistantMessage);
        this.responseChain.add(response);
    }

    public LlmResponse getLlmResponse(String prompt) throws Exception {

        int tokenBudget = 4000;
        try {
            if (this.agent != null && this.agent.getConfig() != null) {
                tokenBudget = this.agent.getConfig().getTokenBudget();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (tokenBudget <= 0) {
            String prop = System.getProperty("march.token.budget");
            if (prop == null || prop.isEmpty()) {
                prop = System.getenv("MARCH_TOKEN_BUDGET");
            }
            if (prop != null && !prop.isEmpty()) {
                try {
                    tokenBudget = Integer.parseInt(prop);
                } catch (NumberFormatException nfe) {
                    System.out.println("Invalid march.token.budget value '" + prop + "', using default: " + tokenBudget);
                }
            }
        }
        trimHistoryToTokenBudget(tokenBudget);

        Schema validator = null;
        try {
            String systemPrompt = agent.getSystemPrompt();
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(systemPrompt);
                com.fasterxml.jackson.databind.JsonNode schemaNode = root.path("marchFramework").path("responseSchema");
                if (!schemaNode.isMissingNode() && !schemaNode.isNull()) {
                    JSONObject rawSchema = new JSONObject(objectMapper.writeValueAsString(schemaNode));
                    validator = SchemaLoader.load(rawSchema);
                }
            }
        } catch (Exception e) {
            // If schema extraction fails, proceed without validator (we still parse into POJO).
            validator = null;
        }

        // Build function definitions for relevant tools and attach them to history as a 'functions' message
        try {
            String lastUser = null;
            for (int i = this.historyMessages.size() - 1; i >= 0; i--) {
                Message m = this.historyMessages.get(i);
                if (m != null && "user".equals(m.getRole())) {
                    lastUser = m.getContent();
                    break;
                }
            }

            int topN = 6;
            try {
                if (agent != null && agent.getConfig() != null) {
                    topN = agent.getConfig().getToolTopN();
                }
            } catch (Exception e) {
                topN = 6;
            }
            Map<String, march.dev.data.ToolDto> relevant = toolRegistry.getRelevantTools(agent.getId(), lastUser, topN);
            if (relevant != null && !relevant.isEmpty()) {
                java.util.List<java.util.Map<String, Object>> functions = march.dev.utils.FunctionDefinitionBuilder.buildFromToolDtos(relevant);
                Message functionsMsg = new Message("functions", objectMapper.writeValueAsString(functions), null, null);
                // remove any previous functions message to keep it fresh
                this.historyMessages.removeIf(msg -> msg != null && "functions".equals(msg.getRole()));
                this.historyMessages.add(functionsMsg);
            }
        } catch (Exception e) {
            // ignore function build errors
        }

        int maxRetries = 2;
        try {
            if (agent != null && agent.getConfig() != null) {
                maxRetries = agent.getConfig().getMaxLlmRetries();
            }
        } catch (Exception e) {
            maxRetries = 2;
        }
        int attempt = 0;
        String lastRaw = null;

        while (true) {
            lastRaw = agent.getLlmResponse(prompt);
            String cleaned = JsonUtils.cleanLlmResponse(lastRaw);

            try {
                if (cleaned == null || cleaned.isEmpty()) {
                    throw new IllegalArgumentException("No JSON found in model output");
                }

                // If a schema validator is available, validate the raw cleaned JSON first.
                if (validator != null) {
                    try {
                        JSONObject candidate = new JSONObject(cleaned);
                        validator.validate(candidate);
                    } catch (ValidationException ve) {
                        throw ve;
                    }
                }

                // Parse into POJO
                LlmResponse response = objectMapper.readValue(cleaned, LlmResponse.class);

                // Basic validation fallback
                if (validator == null && response.getStep() == 0 && (response.getModelAnswer() == null || response.getModelAnswer().isEmpty()) && !response.isFunctionCall()) {
                    throw new IllegalArgumentException("Parsed LlmResponse is missing required content");
                }

                // Success
                this.writeResponseInHistory(response);
                return response;

            } catch (ValidationException | IllegalArgumentException | com.fasterxml.jackson.core.JsonProcessingException parseEx) {
                attempt++;
                if (attempt > maxRetries) {
                    throw new Exception("Unable to validate/parse LLM response into LlmResponse after " + maxRetries + " retries. Last raw output: " + lastRaw, parseEx);
                }

                // Build a short correction prompt that references the schema and gives an example
                String correctionExample = "{\"step\":1,\"modelThought\":\"Think step\",\"modelAnswer\":\"Concise answer\",\"functionCall\":false}";

                String correctionInstruction;
                try {
                    String systemJson = agent.getSystemPrompt();
                    correctionInstruction = systemJson + "\n\nThe previous assistant output could not be parsed/validated as JSON (error: "
                            + parseEx.getMessage() + ").\nPrevious output:\n" + lastRaw
                            + "\nPlease RETURN ONLY a single JSON object that matches the MARCH framework response schema. Example: "
                            + correctionExample + "\nDo not include any explanation or markdown.\n";
                } catch (Exception e) {
                    correctionInstruction = "The previous assistant output could not be parsed/validated as JSON (error: "
                            + parseEx.getMessage() + ").\nPrevious output:\n" + lastRaw
                            + "\nPlease RETURN ONLY a single JSON object that matches the MARCH framework response schema. Example: "
                            + correctionExample + "\nDo not include any explanation or markdown.\n";
                }

                // Insert the correction instruction as a new user message in the history so the model
                // receives the full conversation context (functions, system prompt, etc.) on retry.
                Message correctionMsg = new Message("user", correctionInstruction, null, null);
                this.historyMessages.add(correctionMsg);

                // Recompute the prompt from history so Agent.getLlmResponse receives full messages
                try {
                    prompt = this.getHistory();
                } catch (Exception e) {
                    prompt = correctionInstruction;
                }

                // Small backoff before retrying to avoid immediate hammering
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

                // loop and retry with updated history
                continue;
            }
        }
    }

    private int estimateTokensForMessage(Message m) {
        if (m == null || m.getContent() == null) return 0;
        int chars = m.getContent().length();
        // rough heuristic: 1 token ~= 4 chars
        return Math.max(1, chars / 4 + 3);
    }

    private int estimateTokensForMessages(List<Message> messages) {
        int total = 0;
        if (messages == null) return 0;
        for (Message m : messages) {
            total += estimateTokensForMessage(m);
        }
        return total;
    }

    private void trimHistoryToTokenBudget(int maxTokens) {
        try {
            int estimated = estimateTokensForMessages(this.historyMessages);
            if (estimated <= maxTokens) return;

            // Preserve system message at index 0
            int preserveCount = 1;

            while (estimated > maxTokens && this.historyMessages.size() > preserveCount + 1) {
                // Build a chunk from the oldest non-system messages (take up to 4)
                int take = Math.min(4, this.historyMessages.size() - preserveCount - 0);
                StringBuilder chunk = new StringBuilder();
                int removed = 0;
                for (int i = preserveCount; i < preserveCount + take && i < this.historyMessages.size(); i++) {
                    Message m = this.historyMessages.get(i);
                    chunk.append("[").append(m.getRole()).append("] ").append(m.getContent()).append("\n");
                    removed++;
                }

                if (chunk.length() == 0) break;

                String summPrompt = "Summarize the following conversation chunk concisely in 1-2 sentences. Return only the summary text:\n" + chunk.toString();

                String summary = null;
                try {
                    summary = agent.getLlmResponse(summPrompt);
                    if (summary == null) summary = "";
                } catch (Exception e) {
                    summary = "";
                }

                // Remove the taken messages and replace with a summary message
                for (int i = 0; i < removed; i++) {
                    // always remove at preserveCount index as list shifts
                    if (this.historyMessages.size() > preserveCount)
                        this.historyMessages.remove(preserveCount);
                }

                Message summaryMsg = new Message("summary", summary, null, null);
                this.historyMessages.add(preserveCount, summaryMsg);

                estimated = estimateTokensForMessages(this.historyMessages);
            }
        } catch (Exception e) {
            // best-effort: if trimming fails, fall back to dropping oldest messages until under budget
            while (estimateTokensForMessages(this.historyMessages) > maxTokens && this.historyMessages.size() > 1) {
                this.historyMessages.remove(1);
            }
        }
    }

    public void executeOrder(LlmResponse response) throws Exception {
        Tool tool = toolRegistry.get(response.getToolName());
        Object[] args = response.getOrderedAndTypedArgs(toolRegistry, objectMapper);
        Object result = methodRunner.execute(tool, args);
        String resultJson = objectMapper.writeValueAsString(result);
        Object[] toolArgs = new Object[0];
        if (response.getArguments() != null) {
            toolArgs = response.getArguments().values().toArray();
        }

        SystemResponse systemResponse = new SystemResponse(response.getStep(), response.getToolName(),
            resultJson, toolArgs);
        this.responseChain.add(systemResponse);
        this.writeResponseInHistory(systemResponse);
    }

    public String getSessionId() {
        return sessionId;
    }


    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }


    public Agent getAgent() {
        return agent;
    }


    public void setAgent(Agent agent) {
        this.agent = agent;
    }


    public String getHistory() {
        try {
            java.util.Map<String, Object> wrapper = new java.util.HashMap<>();
            wrapper.put("messages", this.historyMessages);
            return objectMapper.writeValueAsString(wrapper);
        } catch (Exception e) {
            return "";
        }
    }


    public void setHistory(String history) {
        this.historyMessages = new ArrayList<>();
        this.historyMessages.add(new Message("system", history, null, null));
    }


    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }


    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
