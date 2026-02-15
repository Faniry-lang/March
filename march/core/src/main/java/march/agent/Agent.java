package march.agent;

import java.nio.file.Path;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import march.annotations.MarchAgent;
import march.chat.Request;
import march.chat.Response;
import march.chat.State;
import march.chat.Step;
import march.tools.Function;
import march.tools.Tool;
import march.tools.ToolRegistry;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.NotNull;

public class Agent {
    String name;
    String model;
    String apiKey;
    String systemPrompt;
    int contextLength;
    int maxRetriesAllowed = 5;

    private ObjectMapper objectMapper;
    private ToolRegistry toolRegistry;
    private LlmClient llmClient;

    public Agent(int contextLength, String apiKey,
            ObjectMapper objectMapper, ToolRegistry toolRegistry, LlmClient llmClient) {
        if(this.getClass().isAnnotationPresent(MarchAgent.class)) {
            String agentName = this.getClass().getAnnotation(MarchAgent.class).name();
            this.name = (agentName != null && !agentName.isEmpty()) ? agentName : "March Agent";
        }
        this.contextLength = contextLength;
        this.apiKey = apiKey;

        String loaded = "";
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("march/agent/agent.json")) {
            if (is != null) {
                loaded = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
        }
        this.systemPrompt = loaded;
        this.objectMapper = objectMapper;
        this.toolRegistry = toolRegistry;
        this.llmClient = llmClient;
    }

    private Path resolveChatFile(String chatId) {
        String configured = System.getProperty("march.folder");
        Path marchDir = configured != null
                ? java.nio.file.Paths.get(configured)
                : java.nio.file.Paths.get(System.getProperty("user.home"), ".march");
        
        Path chatsDir = marchDir.resolve("chats");
        if (!java.nio.file.Files.exists(chatsDir)) {
             try {
                java.nio.file.Files.createDirectories(chatsDir);
             } catch (IOException e) {
                 e.printStackTrace();
             }
        }
        return chatsDir.resolve(chatId + ".txt");
    }

    public void createNewChat(String chatId) {
        Path chatFile = resolveChatFile(chatId);
        try {
            if (!java.nio.file.Files.exists(chatFile)) {
                java.nio.file.Files.createFile(chatFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeToChatHistory(String userRequest, String agentName, String agentResponse, String chatId) {
        Path chatFile = resolveChatFile(chatId);
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String entry = "[" + timestamp + "] User: " + userRequest + "\n[" + timestamp + "] " + agentName + ": " + agentResponse + "\n--------------------------------------------------\n";
        try {
            java.nio.file.Files.writeString(chatFile, entry, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String chat(String prompt, String chatId, String model) throws Exception {
        long startTime = System.currentTimeMillis();
        String promptId = "MQ-" + (100000 + new java.util.Random().nextInt(900000));
        double totalInputLength = 0;
        double totalOutputLength = 0;
        
        String answer = "";

        StringBuilder toolsInfo = new StringBuilder();
        for (Tool tool : toolRegistry.list()) {
            toolsInfo.append("- name: ").append(tool.getName()).append("\n")
                     .append("  description: ").append(tool.getDescription()).append("\n");
             if (tool instanceof Function f) {
                 toolsInfo.append("  parameters: ").append(f.getParameters().toString()).append("\n");
             }
        }

        int retries = 0;

        ArrayNode messages = objectMapper.createArrayNode();

        String historySummary = getSummaryFromChat(chatId);
        if (!historySummary.isEmpty()) {
            messages.add(objectMapper.createObjectNode().put("role", "system").put("content", "Previous conversation summary/context:\n" + historySummary));
        }

        String metaSystem = getMetaSystem(toolsInfo);

        messages.add(objectMapper.createObjectNode().put("role", "system").put("content", metaSystem));
        messages.add(objectMapper.createObjectNode().put("role", "user").put("content", "User Request: " + prompt));

        while(true) {
            String responseJson = "";
            Response response = null;

            try {
                ObjectNode payload = objectMapper.createObjectNode();
                payload.put("model", model != null ? model : this.model);
                payload.set("messages", messages);

                String payloadStr = objectMapper.writeValueAsString(payload);
                totalInputLength += payloadStr.length();

                String rawLlmResponse = llmClient.call(payloadStr, apiKey);

                JsonNode rootNode = objectMapper.readTree(rawLlmResponse);
                String content = "";
                if (rootNode.has("choices") && rootNode.get("choices").isArray() && rootNode.get("choices").size() > 0) {
                     JsonNode choice = rootNode.get("choices").get(0);
                     if (choice.has("message") && choice.get("message").has("content")) {
                         content = choice.get("message").get("content").asText();
                     }
                }
                
                if (content.isEmpty()) {
                     content = rawLlmResponse;
                }

                totalOutputLength += content.length();
                messages.add(objectMapper.createObjectNode().put("role", "assistant").put("content", content));
                
                responseJson = content;

                int firstBrace = responseJson.indexOf("{");
                int lastBrace = responseJson.lastIndexOf("}");
                if (firstBrace != -1 && lastBrace != -1 && firstBrace <= lastBrace) {
                    responseJson = responseJson.substring(firstBrace, lastBrace + 1);
                }

                response = objectMapper.readValue(responseJson, Response.class);
            } catch (Exception e) {
                if(retries < maxRetriesAllowed) {
                    retries++;

                    messages.add(objectMapper.createObjectNode().put("role", "user").put("content", "Error parsing your previous JSON response: " + e.getMessage() + ". Please try again with valid JSON."));
                    continue;
                }
                throw new Exception("Error during agent chat loop. Last Response Content: " + responseJson, e);
            }
            
            if(response.isToolCall()) {
                Map<String,Object> toolArgs = response.getToolArgs();
                if (toolArgs == null) {
                    throw new Exception("Tool call but no args provided for tool " + response.getToolName());
                }
                if(response.getToolName() == null || !toolRegistry.contains(response.getToolName())) {
                    throw new Exception("Invalid tool call: " + response.getToolName());
                }
                Tool tool = toolRegistry.get(response.getToolName());
                Function fn = (Function) tool;
                Object[] orderedArgs = fn.getParameters().keySet().stream()
                    .map(k -> toolArgs.get(k))
                    .toArray();
                Object toolResult;
                try {
                    toolResult = fn.invoke(orderedArgs);

                    String resultStr = "Tool '" + response.getToolName() + "' Output: " + toolResult;
                    messages.add(objectMapper.createObjectNode().put("role", "user").put("content", resultStr));
                    
                } catch (Exception invokeEx) {
                    System.out.println("[March Framework Error] Tool calling error: "+invokeEx.getMessage());
                    messages.add(objectMapper.createObjectNode().put("role", "user").put("content", "Tool execution failed: " + invokeEx.getMessage()));
                }

            } else {
                answer = response.getFinalAnswer();
                break;
            }
        }

        long endTime = System.currentTimeMillis();
        DetailedPromptInfo info = new DetailedPromptInfo();
        info.setPromptId(promptId);
        info.setAgentName(this.name);
        info.setModel(model != null ? model : this.model);
        info.setChatId(chatId);
        info.setInputTokenLength(totalInputLength);
        info.setOutPutTokenLength(totalOutputLength);
        info.setProcessingTime((double)(endTime - startTime));
        info.setTimestamp(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        CostPerPrompt cost = onAgentUsage(info);
        cost.setPromptId(promptId);
        writeMetrics(cost, info);

        writeToChatHistory(prompt, this.name + " (" + promptId + ")", answer, chatId);
        return answer;
    }

    @NotNull
    private String getMetaSystem(StringBuilder toolsInfo) {
        String userSpec = "Your name is "+ ((this.name != null && !this.name.isEmpty()) ? this.name : "March Agent. ") +
                ((systemPrompt != null && !systemPrompt.isEmpty()) ? "This is the function which the user has assigned to you as an agent: "+
                        this.systemPrompt : "");

        String metaSystem = "You are an autonomous AI agent. You act in a loop to solve a user request.\n" +
                            "You have access to the following tools:\n" + toolsInfo.toString() + "\n" +
                            "You must return a raw JSON object (and ONLY JSON) representing your next action.\n" +
                            "Response Schema: {\"stepId\": int, \"reasoningSummary\": string, \"toolCall\": boolean, \"finalAnswer\": string|null, \"toolName\": string|null, \"toolArgs\": object|null}.\n" +
                            "If you need to use a tool, set toolCall=true, provide the toolName and toolArgs.\n" +
                            "If you have the final answer, set toolCall=false, provide the finalAnswer, and set toolName/toolArgs to null.\n" +
                            "Do not include any markdown formatting (like ```json). Just the raw JSON.\n"+
                            userSpec;
        return metaSystem;
    }

    private String getSummaryFromChat(String chatId) {
        Path chatFile = resolveChatFile(chatId);
        if (!java.nio.file.Files.exists(chatFile)) return "";

        try {
            String history = java.nio.file.Files.readString(chatFile, StandardCharsets.UTF_8);
            if (history.length() > this.contextLength) {
                // TODO: implement a real summary logic using llm call 
                 return history.substring(history.length() - this.contextLength);
            }
            return history;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public march.agent.CostMetrics getCostMetrics() {
        String configured = System.getProperty("march.folder");
        Path marchDir = configured != null
                ? java.nio.file.Paths.get(configured)
                : java.nio.file.Paths.get(System.getProperty("user.home"), ".march");
        
        Path metricsDir = marchDir.resolve("metrics");
        if (!java.nio.file.Files.exists(metricsDir)) {
            return new march.agent.CostMetrics(0.0, 0.0, 0.0, 0.0);
        }

        double dayTotal = 0, weekTotal = 0, monthTotal = 0, yearTotal = 0;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime dayAgo = now.minusDays(1);
        java.time.LocalDateTime weekAgo = now.minusWeeks(1);
        java.time.LocalDateTime monthAgo = now.minusMonths(1);
        java.time.LocalDateTime yearAgo = now.minusYears(1);

        try (java.util.stream.Stream<Path> files = java.nio.file.Files.list(metricsDir)) {
            List<Path> metricFiles = files
                .filter(p -> p.toString().endsWith(".json"))
                .toList();

            for (Path file : metricFiles) {
                try (java.io.InputStream is = java.nio.file.Files.newInputStream(file)) {
                    com.fasterxml.jackson.databind.MappingIterator<JsonNode> it = objectMapper.readerFor(JsonNode.class).readValues(is);
                    while (it.hasNextValue()) {
                        try {
                            JsonNode node = it.nextValue();
                            if (!node.has("agentName") || !node.get("agentName").asText().equals(this.name)) {
                                continue;
                            }

                            String tsStr = node.get("timestamp").asText();
                            java.time.LocalDateTime ts = java.time.LocalDateTime.parse(tsStr);
                            double cost = node.get("apiCost").asDouble() + node.get("computeCost").asDouble();

                            if (ts.isAfter(dayAgo)) dayTotal += cost;
                            if (ts.isAfter(weekAgo)) weekTotal += cost;
                            if (ts.isAfter(monthAgo)) monthTotal += cost;
                            if (ts.isAfter(yearAgo)) yearTotal += cost;
                        } catch (Exception e) {
                            System.err.println("[March Framework Error] Cost Metrics line parsing error: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[March Framework Error] Cost Metrics file reading error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new march.agent.CostMetrics(dayTotal, weekTotal, monthTotal, yearTotal);
    }

    public static class CostPerPrompt {
        String promptId;
        Double apiCost;
        Double computeCost;

        public String getPromptId() {
            return promptId;
        }

        public void setPromptId(String promptId) {
            this.promptId = promptId;
        }

        public Double getApiCost() {
            return apiCost;
        }

        public void setApiCost(Double apiCost) {
            this.apiCost = apiCost;
        }

        public Double getComputeCost() {
            return computeCost;
        }

        public void setComputeCost(Double computeCost) {
            this.computeCost = computeCost;
        }
    }

    public static class DetailedPromptInfo {
        String promptId;
        Double inputTokenLength;
        Double outPutTokenLength;
        String agentName;
        String model;
        String chatId;
        String timestamp;
        Double processingTime;

        public String getPromptId() {
            return promptId;
        }

        public void setPromptId(String promptId) {
            this.promptId = promptId;
        }

        public Double getInputTokenLength() {
            return inputTokenLength;
        }

        public void setInputTokenLength(Double inputTokenLength) {
            this.inputTokenLength = inputTokenLength;
        }

        public Double getOutPutTokenLength() {
            return outPutTokenLength;
        }

        public void setOutPutTokenLength(Double outPutTokenLength) {
            this.outPutTokenLength = outPutTokenLength;
        }

        public String getAgentName() {
            return agentName;
        }

        public void setAgentName(String agentName) {
            this.agentName = agentName;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getChatId() {
            return chatId;
        }

        public void setChatId(String chatId) {
            this.chatId = chatId;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public Double getProcessingTime() {
            return processingTime;
        }

        public void setProcessingTime(Double processingTime) {
            this.processingTime = processingTime;
        }
    }

    private void writeMetrics(CostPerPrompt promptCost, DetailedPromptInfo detailedInfo) {
        String configured = System.getProperty("march.folder");
        Path marchDir = configured != null
                ? java.nio.file.Paths.get(configured)
                : java.nio.file.Paths.get(System.getProperty("user.home"), ".march");
        
        Path metricsDir = marchDir.resolve("metrics");
        if (!java.nio.file.Files.exists(metricsDir)) {
             try {
                java.nio.file.Files.createDirectories(metricsDir);
             } catch (IOException e) {
                 e.printStackTrace();
                 return;
             }
        }

        String fileName = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HH")) + ".json";
        Path metricFile = metricsDir.resolve(fileName);

        try {
            ObjectNode entry = objectMapper.createObjectNode();
            entry.put("promptId", detailedInfo.getPromptId());
            entry.put("agentName", detailedInfo.getAgentName());
            entry.put("model", detailedInfo.getModel());
            entry.put("inputLength", detailedInfo.getInputTokenLength());
            entry.put("outputLength", detailedInfo.getOutPutTokenLength());
            entry.put("processingTimeMs", detailedInfo.getProcessingTime());
            entry.put("apiCost", promptCost.getApiCost() != null ? promptCost.getApiCost() : 0.0);
            entry.put("computeCost", promptCost.getComputeCost() != null ? promptCost.getComputeCost() : 0.0);
            entry.put("timestamp", detailedInfo.getTimestamp());
            entry.put("chatId", detailedInfo.getChatId());

            String jsonLine = objectMapper.writer()
                .without(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(entry) + "\n";
            java.nio.file.Files.writeString(metricFile, jsonLine, StandardCharsets.UTF_8, 
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CostPerPrompt onAgentUsage(DetailedPromptInfo info) {
        return new CostPerPrompt();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public int getMaxRetriesAllowed() {
        return maxRetriesAllowed;
    }

    public void setMaxRetriesAllowed(int maxRetriesAllowed) {
        this.maxRetriesAllowed = maxRetriesAllowed;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }

    public void setToolRegistry(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public LlmClient getLlmClient() {
        return llmClient;
    }

    public void setLlmClient(LlmClient llmClient) {
        this.llmClient = llmClient;
    }
}