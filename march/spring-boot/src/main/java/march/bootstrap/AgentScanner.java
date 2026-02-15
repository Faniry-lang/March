package march.bootstrap;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import march.agent.Agent;
import march.annotations.MarchAgent;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class AgentScanner {

    private final ApplicationContext context;

    public AgentScanner(ApplicationContext context) {
        this.context = context;
    }

    public List<Class<? extends Agent>> scanForAgents(String basePackage) {
        List<Class<? extends Agent>> agentClasses = new ArrayList<>();
        
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(MarchAgent.class));

        Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
        for (BeanDefinition bd : candidates) {
            try {
                Class<?> cls = Class.forName(bd.getBeanClassName());
                if (Agent.class.isAssignableFrom(cls)) {
                    agentClasses.add((Class<? extends Agent>) cls);
                }
            } catch (ClassNotFoundException e) {
          
                e.printStackTrace();
            }
        }
        return agentClasses;
    }
}
