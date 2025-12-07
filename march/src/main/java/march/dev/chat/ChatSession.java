package march.dev.chat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import march.dev.agent.Agent;
import march.dev.data.Tool;
import march.dev.data.ToolRegistry;
import march.dev.process.LlmResponse;
import march.dev.process.Response;
import march.dev.process.SystemResponse;
import march.dev.utils.JsonUtils;
import march.dev.utils.MethodRunner;
import march.dev.utils.TemplateUtils;

public class ChatSession {
    
    String sessionId;
    Agent agent;
    String history;
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
        StringBuilder sb = new StringBuilder();
        this.history = this.agent.getSystemPrompt();
        sb.append("<chat-session>");
        sb.append("<history>");
            sb.append("<history-placeholder/>");
        sb.append("</history>");
        sb.append("<current-request-process>");
            sb.append("<user-request>");
                sb.append("<user-request-placeholder/>");
            sb.append("</user-request>");
            sb.append("<chain-of-thought>");
                sb.append("<chain-placeholder/>");
            sb.append("</chain-of-thought>");
        sb.append("</current-request-process>");
        sb.append("</chat-session>");
        this.history += sb.toString();
    }

    public void setUserRequest(String userRequest) {
        this.history = TemplateUtils.replace(this.history, "<user-request-placeholder/>", userRequest);
    }

    public void endUserRequest() {
        String currentUserRequestContent = TemplateUtils.getTagsContent(this.history, "user-request", false);
        if (currentUserRequestContent == null) {
            currentUserRequestContent = "";
        }
        
        String currentChainOfThoughtContent = TemplateUtils.getTagsContent(this.history, "chain-of-thought", false);
        if (currentChainOfThoughtContent == null) {
            currentChainOfThoughtContent = "";
        }
        
        String wholeCurrentRequestProcessContent = TemplateUtils.getTagsContent(this.history, "current-request-process", false);

        String processRequestToAddToHistory;
        if (wholeCurrentRequestProcessContent != null) {
            processRequestToAddToHistory = "<history>"+wholeCurrentRequestProcessContent+"</history><history-placeholder/>";
        } else {
            processRequestToAddToHistory = "<history-placeholder/>";
        }

        LinkedHashMap<String, String> replacement = new LinkedHashMap<>();
        
        replacement.put(currentUserRequestContent, "<user-request-placeholder/>");
        replacement.put(currentChainOfThoughtContent, "<chain-placeholder/>");
        replacement.put("<history-placeholder/>", processRequestToAddToHistory);

        this.history = TemplateUtils.multiReplace(this.history, replacement);
    }

    public void initResponseChain() {
        this.responseChain = new ArrayList<>();
    }

    public void writeResponseInHistory(Response response) throws Exception {
        String responseTemplate = response.toXml();
        responseTemplate += "<chain-placeholder/>";
        this.history = TemplateUtils.replace(this.history, "<chain-placeholder/>", responseTemplate);
    }

    public LlmResponse getLlmResponse(String prompt) throws Exception {
        String llmResponseString = agent.getLlmResponse(prompt);
        String cleanedLlmResponseString = JsonUtils.cleanLlmResponse(llmResponseString);
        LlmResponse response = objectMapper.readValue(cleanedLlmResponseString, LlmResponse.class);
        this.responseChain.add(response);
        this.writeResponseInHistory(response);
        return response;
    }

    public void executeOrder(LlmResponse response) throws Exception {
        Tool tool = toolRegistry.get(response.getToolName());
        Object[] args = response.getOrderedAndTypedArgs(toolRegistry, objectMapper);
        Object result = methodRunner.execute(tool, args);
        String resultJson = objectMapper.writeValueAsString(result);
        SystemResponse systemResponse = new SystemResponse(response.getStep(), response.getToolName(),
                resultJson, response.getArguments().values().toArray());
        this.responseChain.add(systemResponse);
        this.writeResponseInHistory(response);
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
        return history;
    }


    public void setHistory(String history) {
        this.history = history;
    }


    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }


    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
