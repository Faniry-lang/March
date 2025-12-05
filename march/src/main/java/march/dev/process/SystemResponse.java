package march.dev.process;

public class SystemResponse {
    int step;
    String toolName;
    String toolOutput;
    Object[] toolArgs;

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
