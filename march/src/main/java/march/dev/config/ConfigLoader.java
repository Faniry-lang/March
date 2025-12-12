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
        String basePath = "src/main/resources/march-agents/";

        try (FileInputStream fis = new FileInputStream("src/main/resources/march.properties")) {
            props.load(fis);
            basePath = props.getProperty("march.agents.path", basePath);
        } catch (IOException e) {
            System.out.println("Could not load march.properties, using default path: " + basePath);
        }

        if (!basePath.endsWith("/")) {
            basePath += "/";
        }

        String filePath = basePath + agentId + ".json";

        try {
            AgentConfig config = objectMapper.readValue(new File(filePath), AgentConfig.class);
            
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
