package march.examples.spring_boot_project;

import march.examples.spring_boot_project.agents.FinanceAgent;
import march.examples.spring_boot_project.cli.SpringBootMarchCLI;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Autowired;
import march.examples.spring_boot_project.services.SampleService;

import java.util.Scanner;
import march.bootstrap.SpringBootMarchAgentFactory;
import march.agent.Agent;
import march.tools.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine;

@SpringBootApplication
public class SpringBootProjectApplication implements CommandLineRunner {

    @Autowired
    private SpringBootMarchAgentFactory agentFactory;
    @Autowired
    private SpringBootMarchCLI cli;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SpringBootProjectApplication.class);
        app.run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0) {
            new picocli.CommandLine(cli).execute(args);
        } else {
            cli.run();
            
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print(picocli.CommandLine.Help.Ansi.AUTO.string("@|bold,cyan march> |@"));
                if (!scanner.hasNextLine()) break;
                String line = scanner.nextLine().trim();
                if ("exit".equals(line) || "quit".equals(line)) break;
                if (line.isEmpty()) continue;
                
                String[] commandArgs = line.split("\\s+");
                new picocli.CommandLine(cli).execute(commandArgs);
            }
        }
    }
}
