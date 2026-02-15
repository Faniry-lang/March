package march.chat;

import march.tools.Tool;

public class Request {

    private String model;
    private String systemPrompt;
    private String message;
    private Step[] steps;
    private State currentState;
    private Tool[] tools;

    private Spec spec;
    public Request() {
    }
    public Request(String model, String systemPrompt, String message, Step[] steps, State currentState, Tool[] tools, Spec spec) {
        this.model = model;
        this.systemPrompt = systemPrompt;
        this.message = message;
        this.steps = steps;
        this.currentState = currentState;
        this.tools = tools;
        this.spec = spec;
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
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public Step[] getSteps() {
        return steps;
    }
    public void setSteps(Step[] steps) {
        this.steps = steps;
    }
    public State getCurrentState() {
        return currentState;
    }
    public void setCurrentState(State currentState) {
        this.currentState = currentState;
    }
    public Tool[] getTools() {
        return tools;
    }
    public void setTools(Tool[] tools) {
        this.tools = tools;
    }
    public Spec getSpec() {
        return spec;
    }
    public void setSpec(Spec spec) {
        this.spec = spec;
    }
}
