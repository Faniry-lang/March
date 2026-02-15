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

public interface AgentFactory {
     <T extends Agent> T createAgent(Class<T> agentClass);
}
