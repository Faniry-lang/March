package march.bootstrap;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import march.agent.Agent;
import march.agent.LlmClient;
import march.agent.OpenRouterClient;
import march.tools.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SpringBootMarchAgentFactory implements AgentFactory {

    private final ApplicationContext applicationContext;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final Map<String, Agent> agentCache = new ConcurrentHashMap<>();

    public SpringBootMarchAgentFactory(ApplicationContext applicationContext, ToolRegistry toolRegistry, ObjectMapper objectMapper) {
        this.applicationContext = applicationContext;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T extends Agent> T createAgent(Class<T> agentClass) {
        String agentName = agentClass.getName();
        
        if (agentCache.containsKey(agentName)) {
            return agentClass.cast(agentCache.get(agentName));
        }

        try {
            String apiKey = System.getenv("OPENROUTER_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                apiKey = System.getProperty("march.api.key");
                if (apiKey == null) {
                    throw new RuntimeException("OPENROUTER_API_KEY not found in environment or system properties.");
                }
            }

            LlmClient llmClient = new OpenRouterClient();

            Constructor<T> constructor = agentClass.getConstructor(String.class, ObjectMapper.class, ToolRegistry.class, LlmClient.class);
            T agentInstance = constructor.newInstance(apiKey, objectMapper, toolRegistry, llmClient);

            agentCache.put(agentName, agentInstance);
            return agentInstance;

        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Failed to instantiate agent " + agentName + ". Ensure it has a public constructor(String apiKey, ObjectMapper objectMapper, ToolRegistry toolRegistry, LlmClient llmClient).", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create agent: " + agentName, e);
        }
    }
}
