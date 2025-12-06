package march.dev.config;

import java.util.Map;

public class AgentConfig {
    private String model;
    private Map<String, String> systemInstruction;

    public AgentConfig() {
    }

    public AgentConfig(String model, Map<String, String> systemInstruction) {
        this.model = model;
        this.systemInstruction = systemInstruction;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Map<String, String> getSystemInstruction() {
        return systemInstruction;
    }

    public void setSystemInstruction(Map<String, String> systemInstruction) {
        this.systemInstruction = systemInstruction;
    }
}
