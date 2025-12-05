package march.dev.process;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import march.dev.data.Tool;
import march.dev.data.ToolRegistry;

public class LlmResponse {
    int step;
    String modelThought;
    String modelAnswer;
    boolean functionCall;
    String toolName;
    Map<String, Object> arguments;

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
