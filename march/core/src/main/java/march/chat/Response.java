package march.chat;

import java.util.LinkedHashMap;

public class Response {

    private int stepId;

    private String reasoningSummary;

    private boolean toolCall;
    private String finalAnswer;

    private String toolName;
    private LinkedHashMap<String, Object> toolArgs;

    public Response(int stepId, String reasoningSummary, boolean toolCall, String finalAnswer, String toolName,
            LinkedHashMap<String, Object> toolArgs) {
        this.stepId = stepId;
        this.reasoningSummary = reasoningSummary;
        this.toolCall = toolCall;
        this.finalAnswer = finalAnswer;
        this.toolName = toolName;
        this.toolArgs = toolArgs;
    }

    public Response() {
    }

    public int getStepId() {
        return stepId;
    }
    public void setStepId(int stepId) {
        this.stepId = stepId;
    }
    public String getReasoningSummary() {
        return reasoningSummary;
    }
    public void setReasoningSummary(String reasoningSummary) {
        this.reasoningSummary = reasoningSummary;
    }
    public boolean isToolCall() {
        return toolCall;
    }
    public void setToolCall(boolean toolCall) {
        this.toolCall = toolCall;
    }
    public String getFinalAnswer() {
        return finalAnswer;
    }
    public void setFinalAnswer(String finalAnswer) {
        this.finalAnswer = finalAnswer;
    }
    public String getToolName() {
        return toolName;
    }
    public void setToolName(String toolName) {
        this.toolName = toolName;
    }
    public LinkedHashMap<String, Object> getToolArgs() {
        return toolArgs;
    }
    public void setToolArgs(LinkedHashMap<String, Object> toolArgs) {
        this.toolArgs = toolArgs;
    }
}
