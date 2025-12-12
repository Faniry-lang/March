package march.dev.config;

import java.util.Map;

public class AgentConfig {
    private String model;
    private Map<String, String> systemInstruction;
    private int maxHistorySize = 10000;
    private int maxActiveSummaries = 5;
    private int tokenBudget = 4000;
    // LLM client network and retry settings (seconds / milliseconds)
    private int connectTimeoutSec = 15;
    private int readTimeoutSec = 60;
    private int writeTimeoutSec = 60;
    private int callTimeoutSec = 120;
    private long llmInitialBackoffMs = 500;
    private String provider;
    private Map<String, String> providerConfig;
    private int toolTopN = 6;
    private int maxLlmRetries = 2;
    private long cacheTtlMs = 5 * 60 * 1000; // default 5 minutes
    private boolean historyTransientEnabled = true;
    private String historyDir;

    public AgentConfig() {
    }

    public AgentConfig(String model, Map<String, String> systemInstruction, int maxHistorySize,
            int maxActiveSummaries) {
        this.model = model;
        this.systemInstruction = systemInstruction;
        this.maxHistorySize = maxHistorySize;
        this.maxActiveSummaries = maxActiveSummaries;
    }

    public AgentConfig(String model, Map<String, String> systemInstruction, int maxHistorySize,
            int maxActiveSummaries, int tokenBudget) {
        this.model = model;
        this.systemInstruction = systemInstruction;
        this.maxHistorySize = maxHistorySize;
        this.maxActiveSummaries = maxActiveSummaries;
        this.tokenBudget = tokenBudget;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Map<String, String> getProviderConfig() {
        return providerConfig;
    }

    public void setProviderConfig(Map<String, String> providerConfig) {
        this.providerConfig = providerConfig;
    }

    public int getToolTopN() {
        return toolTopN;
    }

    public void setToolTopN(int toolTopN) {
        this.toolTopN = toolTopN;
    }

    public int getMaxLlmRetries() {
        return maxLlmRetries;
    }

    public void setMaxLlmRetries(int maxLlmRetries) {
        this.maxLlmRetries = maxLlmRetries;
    }

    public long getCacheTtlMs() {
        return cacheTtlMs;
    }

    public void setCacheTtlMs(long cacheTtlMs) {
        this.cacheTtlMs = cacheTtlMs;
    }

    public boolean isHistoryTransientEnabled() {
        return historyTransientEnabled;
    }

    public void setHistoryTransientEnabled(boolean historyTransientEnabled) {
        this.historyTransientEnabled = historyTransientEnabled;
    }

    public String getHistoryDir() {
        return historyDir;
    }

    public void setHistoryDir(String historyDir) {
        this.historyDir = historyDir;
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

    public int getConnectTimeoutSec() {
        return connectTimeoutSec;
    }

    public void setConnectTimeoutSec(int connectTimeoutSec) {
        this.connectTimeoutSec = connectTimeoutSec;
    }

    public int getReadTimeoutSec() {
        return readTimeoutSec;
    }

    public void setReadTimeoutSec(int readTimeoutSec) {
        this.readTimeoutSec = readTimeoutSec;
    }

    public int getWriteTimeoutSec() {
        return writeTimeoutSec;
    }

    public void setWriteTimeoutSec(int writeTimeoutSec) {
        this.writeTimeoutSec = writeTimeoutSec;
    }

    public int getCallTimeoutSec() {
        return callTimeoutSec;
    }

    public void setCallTimeoutSec(int callTimeoutSec) {
        this.callTimeoutSec = callTimeoutSec;
    }

    public long getLlmInitialBackoffMs() {
        return llmInitialBackoffMs;
    }

    public void setLlmInitialBackoffMs(long llmInitialBackoffMs) {
        this.llmInitialBackoffMs = llmInitialBackoffMs;
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

    public int getTokenBudget() {
        return tokenBudget;
    }

    public void setTokenBudget(int tokenBudget) {
        this.tokenBudget = tokenBudget;
    }
}
