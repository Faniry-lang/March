package march.examples.spring_boot_project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import com.fasterxml.jackson.databind.ObjectMapper;

import march.bootstrap.MarchInitializer;
import march.utils.SpringBootProviderScan;
import march.tools.ToolRegistry;
import march.bootstrap.SpringBootMarchAgentFactory;
import march.bootstrap.AgentScanner;

@Configuration
public class MarchConfig {
    @Bean
    public ToolRegistry toolRegistry() {
        return new ToolRegistry();
    }
    @Bean
    public SpringBootProviderScan springBootProviderScan(ApplicationContext context, ToolRegistry registry) {
        return new SpringBootProviderScan(context, registry);
    }
    @Bean 
    public MarchInitializer marchInitializer(ToolRegistry registry, Environment env, ObjectMapper mapper) {
        return new MarchInitializer(registry, env, mapper);
    }
    
    @Bean
    public SpringBootMarchAgentFactory springBootMarchAgentFactory(ApplicationContext context, ToolRegistry registry, ObjectMapper mapper) {
        return new SpringBootMarchAgentFactory(context, registry, mapper);
    }

    @Bean
    public AgentScanner agentScanner(ApplicationContext context) {
        return new AgentScanner(context);
    }
}