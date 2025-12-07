package march.dev.process;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import march.dev.data.Tool;
import march.dev.data.ToolRegistry;
import march.dev.utils.TemplateUtils;

public class LlmResponse implements Response {
    int step;
    String modelThought;
    String modelAnswer;
    boolean functionCall;
    String toolName;
    Map<String, Object> arguments;

    @Override
    public String toXml() throws Exception {
        String content = this.contentToXml();
        String messageTemplate = TemplateUtils.getMessageTemplate();
        messageTemplate = TemplateUtils.replace(messageTemplate, "<content-placeholder/>", content);
        messageTemplate = TemplateUtils.replace(messageTemplate, "<role-placeholder/>", "agent");
        return messageTemplate;
    }

    public String contentToXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<llm-response>");
        sb.append("<step>").append(this.step).append("</step>");
        sb.append("<modelThought>").append(this.modelThought).append("</modelThought>");
        sb.append("<tool-details>");
        sb.append("<functionCall>").append(this.functionCall).append("</functionCall>");
        if (this.functionCall) {
            sb.append("<toolName>").append(this.toolName).append("</toolName>");
            sb.append("<arguments>");
            if (this.arguments != null) {
                for (Map.Entry<String, Object> entry : this.arguments.entrySet()) {
                    sb.append("<").append(entry.getKey()).append(">");
                    sb.append(entry.getValue());
                    sb.append("</").append(entry.getKey()).append(">");
                }
            }
            sb.append("</arguments>");
        }
        sb.append("</tool-details>");
        sb.append("<modelAnswer>").append(this.modelAnswer).append("</modelAnswer>");
        sb.append("</llm-response>");
        return sb.toString();
    }

    public LlmResponse() {
    }

    public LlmResponse(int step, String modelThought, String modelAnswer, boolean functionCall, String toolName,
            Map<String, Object> arguments) {
        this.step = step;
        this.modelThought = modelThought;
        this.modelAnswer = modelAnswer;
        this.functionCall = functionCall;
        this.toolName = toolName;
        this.arguments = arguments;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public String getModelThought() {
        return modelThought;
    }

    public void setModelThought(String modelThought) {
        this.modelThought = modelThought;
    }

    public String getModelAnswer() {
        return modelAnswer;
    }

    public void setModelAnswer(String modelAnswer) {
        this.modelAnswer = modelAnswer;
    }

    public boolean isFunctionCall() {
        return functionCall;
    }

    public void setFunctionCall(boolean functionCall) {
        this.functionCall = functionCall;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }

    public Object[] getOrderedAndTypedArgs(ToolRegistry toolRegistry, ObjectMapper objectMapper) throws Exception {
        if (!functionCall || toolName == null || toolName.isEmpty()) {
            return new Object[0];
        }

        Tool tool = toolRegistry.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("Tool not found: " + toolName);
        }

        Map<String, java.lang.reflect.Type> paramTypes = tool.getParams();
        List<Object> orderedArgs = new ArrayList<>();

        for (Map.Entry<String, java.lang.reflect.Type> entry : paramTypes.entrySet()) {
            String paramName = entry.getKey();
            java.lang.reflect.Type paramType = entry.getValue();

            if (arguments.containsKey(paramName)) {
                Object argValue = arguments.get(paramName);

                Object typedArg = objectMapper.convertValue(argValue, objectMapper.constructType(paramType));
                orderedArgs.add(typedArg);
            } else {
                orderedArgs.add(null);
            }
        }

        return orderedArgs.toArray();
    }
}
