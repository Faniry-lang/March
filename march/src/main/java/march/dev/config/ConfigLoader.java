package march.dev.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static AgentConfig loadConfig(String agentId) throws IOException {
        Properties props = new Properties();
        // Resolution order for agent config path:
        // 1) System property -Dmarch.agents.path
        // 2) Env var MARCH_AGENTS_PATH
        // 3) march.properties (if present)
        // 4) classpath resource march-agents/<agentId>.json

        String basePath = System.getProperty("march.agents.path");
        if (basePath == null || basePath.isBlank()) {
            basePath = System.getenv("MARCH_AGENTS_PATH");
        }

        // Load march.properties only if no explicit path provided
        if (basePath == null || basePath.isBlank()) {
            try (FileInputStream fis = new FileInputStream("src/main/resources/march.properties")) {
                props.load(fis);
                basePath = props.getProperty("march.agents.path");
            } catch (IOException e) {
                // march.properties missing is acceptable; we'll try classpath later
            }
        }

        if (basePath != null && !basePath.isBlank()) {
            if (!basePath.endsWith("/")) basePath += "/";
        }

        String filePath = basePath != null && !basePath.isBlank() ? basePath + agentId + ".json" : null;

        try {
            AgentConfig config = null;
            if (filePath != null) {
                java.nio.file.Path p = java.nio.file.Paths.get(filePath);
                if (java.nio.file.Files.exists(p)) {
                    config = objectMapper.readValue(p.toFile(), AgentConfig.class);
                }
            }

            // If not found on filesystem, try classpath resource
            if (config == null) {
                String resourcePath = "march-agents/" + agentId + ".json";
                java.io.InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
                if (is != null) {
                    config = objectMapper.readValue(is, AgentConfig.class);
                }
            }

            // If still null, throw to be handled by the catch below which returns default
            if (config == null) throw new java.io.IOException("Agent config not found for '" + agentId + "' (tried filesystem and classpath)");
            
            // If tokenBudget is not set or invalid in the file, load from properties or env
            if (config.getTokenBudget() <= 0) {
                String propBudget = props.getProperty("march.token.budget");
                if (propBudget == null || propBudget.isEmpty()) {
                    propBudget = System.getenv("MARCH_TOKEN_BUDGET");
                }
                if (propBudget != null && !propBudget.isEmpty()) {
                    try {
                        int tb = Integer.parseInt(propBudget);
                        if (tb > 0) config.setTokenBudget(tb);
                    } catch (NumberFormatException nfe) {
                        System.out.println("Invalid token budget in properties/env: " + propBudget);
                    }
                }
            }

            // toolTopN fallback
            if (config.getToolTopN() <= 0) {
                String prop = props.getProperty("march.tool.topn");
                if (prop == null || prop.isEmpty()) prop = System.getenv("MARCH_TOOL_TOPN");
                if (prop != null && !prop.isEmpty()) {
                    try {
                        int v = Integer.parseInt(prop);
                        if (v > 0) config.setToolTopN(v);
                    } catch (NumberFormatException nfe) {
                        System.out.println("Invalid march.tool.topn value: " + prop);
                    }
                }
            }

            // maxLlmRetries fallback
            if (config.getMaxLlmRetries() <= 0) {
                String prop = props.getProperty("march.max.llm.retries");
                if (prop == null || prop.isEmpty()) prop = System.getenv("MARCH_MAX_LLM_RETRIES");
                if (prop != null && !prop.isEmpty()) {
                    try {
                        int v = Integer.parseInt(prop);
                        if (v > 0) config.setMaxLlmRetries(v);
                    } catch (NumberFormatException nfe) {
                        System.out.println("Invalid march.max.llm.retries value: " + prop);
                    }
                }
            }

            // cacheTtlMs fallback
            if (config.getCacheTtlMs() <= 0) {
                String prop = props.getProperty("march.cache.ttl.ms");
                if (prop == null || prop.isEmpty()) prop = System.getenv("MARCH_CACHE_TTL_MS");
                if (prop != null && !prop.isEmpty()) {
                    try {
                        long v = Long.parseLong(prop);
                        if (v > 0) config.setCacheTtlMs(v);
                    } catch (NumberFormatException nfe) {
                        System.out.println("Invalid march.cache.ttl.ms value: " + prop);
                    }
                }
            }

            // provider fallback
            if (config.getProvider() == null || config.getProvider().isEmpty()) {
                String prop = props.getProperty("march.provider");
                if (prop == null || prop.isEmpty()) prop = System.getenv("MARCH_PROVIDER");
                if (prop != null && !prop.isEmpty()) {
                    config.setProvider(prop);
                }
            }

            return config;
        } catch (IOException e) {
            System.out.println("Could not load agent config for '" + agentId + "' from " + filePath + ": " + e.getMessage());
            // Return a default config to avoid NPEs elsewhere
            return new AgentConfig();
        }
    }
}
