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
        return objectMapper.readValue(new File(filePath), AgentConfig.class);
    }
}
