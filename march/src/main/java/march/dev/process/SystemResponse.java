package march.dev.process;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SystemResponse implements Response {
    int step;
    String toolName;
    String toolOutput;
    Object[] toolArgs;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String toJson() throws Exception {
        java.util.Map<String, Object> root = new java.util.LinkedHashMap<>();
        root.put("step", this.step);
        root.put("toolName", this.toolName);
        root.put("toolOutput", this.toolOutput);
        root.put("toolArgs", this.toolArgs != null ? this.toolArgs : new Object[0]);
        return MAPPER.writeValueAsString(root);
    }

    public SystemResponse(int step, String toolName, String toolOutput, Object[] toolArgs) {
        this.step = step;
        this.toolName = toolName;
        this.toolOutput = toolOutput;
        this.toolArgs = toolArgs;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolOutput() {
        return toolOutput;
    }

    public void setToolOutput(String toolOutput) {
        this.toolOutput = toolOutput;
    }

    public Object[] getToolArgs() {
        return toolArgs;
    }

    public void setToolArgs(Object[] toolArgs) {
        this.toolArgs = toolArgs;
    }
}
