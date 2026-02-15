package march.chat;

import java.util.LinkedHashMap;

public class State extends Response {

    public State(Response response, Object result) {
        super(response.getStepId(), response.getReasoningSummary(), response.isToolCall(),
                response.getFinalAnswer(), response.getToolName(), response.getToolArgs());
        this.result = result;
    }

    public State(int stepId, String reasoningSummary, boolean toolCall, String finalAnswer, String toolName,
            LinkedHashMap<String, Object> toolArgs, Object result) {
        super(stepId, reasoningSummary, toolCall, finalAnswer, toolName, toolArgs);
        this.result = result;
    }

    Object result;

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
