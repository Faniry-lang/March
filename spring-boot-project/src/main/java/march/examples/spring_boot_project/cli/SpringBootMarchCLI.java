package march.examples.spring_boot_project.cli;

import march.cli.MarchCLI;
import march.cli.AgentCommand;
import picocli.CommandLine;
import march.agent.Agent;
import march.bootstrap.SpringBootMarchAgentFactory;
import march.bootstrap.AgentScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Component
@CommandLine.Command(name = "spring-march",
        description = "March CLI for Spring Boot Project",
        mixinStandardHelpOptions = true) 
public class SpringBootMarchCLI extends MarchCLI {

    private final SpringBootMarchAgentFactory agentFactory;
    private final AgentScanner agentScanner;

    @Autowired
    public SpringBootMarchCLI(SpringBootMarchAgentFactory agentFactory, AgentScanner agentScanner) {
        this.agentFactory = agentFactory;
        this.agentScanner = agentScanner;
    }

    @PostConstruct
    public void init() {
        AgentCommand.agentScannerSupplier = (pkg) -> (List) agentScanner.scanForAgents(pkg);
        AgentCommand.agentCreatorSupplier = (cls, v) -> agentFactory.createAgent((Class<? extends Agent>) cls);
    }
}
