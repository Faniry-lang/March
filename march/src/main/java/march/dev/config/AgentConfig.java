package march.dev.config;

import java.util.Map;

public class AgentConfig {
    private String model;
    private Map<String, String> systemInstruction;
    private int maxHistorySize = 10000;
    private int maxActiveSummaries = 5;

    public AgentConfig() {
    }

    public AgentConfig(String model, Map<String, String> systemInstruction, int maxHistorySize,
            int maxActiveSummaries) {
        this.model = model;
        this.systemInstruction = systemInstruction;
        this.maxHistorySize = maxHistorySize;
        this.maxActiveSummaries = maxActiveSummaries;
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

    public int getMaxHistorySize() {
        return maxHistorySize;
    }

    public void setMaxHistorySize(int maxHistorySize) {
        this.maxHistorySize = maxHistorySize;
    }

    public int getMaxActiveSummaries() {
        return maxActiveSummaries;
    }

    public void setMaxActiveSummaries(int maxActiveSummaries) {
        this.maxActiveSummaries = maxActiveSummaries;
    }
}
