package march.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import march.agent.Agent;
import java.util.concurrent.Callable;
import java.util.Scanner;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

@Command(name = "agent", description = "Commands to manage and interact with agents")
public class AgentCommand implements Callable<Integer> {

    public static Function<String, List<Class<? extends Agent>>> agentScannerSupplier;
    public static BiFunction<Class<? extends Agent>, Void, Agent> agentCreatorSupplier;

    @picocli.CommandLine.ParentCommand
    private MarchCLI parent;

    @Command(name = "list", description = "List all available agents and their cost metrics")
    public Integer list() {
        System.out.println(picocli.CommandLine.Help.Ansi.AUTO.string("@|bold,green " +
                String.format("%-20s %-15s %-15s %-15s %-15s", "Agent Name", "Cost/Day", "Cost/Week", "Cost/Month", "Cost/Year") + "|@"));
        System.out.println(picocli.CommandLine.Help.Ansi.AUTO.string("@|faint ---------------------------------------------------------------------------------------|@"));
        
        if (agentScannerSupplier == null || agentCreatorSupplier == null) {
            System.out.println(picocli.CommandLine.Help.Ansi.AUTO.string("@|bold,red Error: Agent services not initialized in CLI context.|@"));
            return 1;
        }

        List<Class<? extends Agent>> agents = agentScannerSupplier.apply("march");
        
        for (Class<? extends Agent> agentClass : agents) {
            try {
                Agent agent = agentCreatorSupplier.apply(agentClass, null);
                march.agent.CostMetrics metrics = agent.getCostMetrics();
                String row = String.format("%-20s @|cyan $%-14.2f|@ @|cyan $%-14.2f|@ @|cyan $%-14.2f|@ @|cyan $%-14.2f|@", 
                        agent.getName(), 
                        metrics.getPerDay(), 
                        metrics.getPerWeek(), 
                        metrics.getPerMonth(), 
                        metrics.getPerYear());
                System.out.println(picocli.CommandLine.Help.Ansi.AUTO.string(row));
            } catch (Exception e) {
                System.out.println(picocli.CommandLine.Help.Ansi.AUTO.string("@|red Error loading " + agentClass.getSimpleName() + ": " + e.getMessage() + "|@"));
            }
        }
        return 0;
    }

    @Command(name = "chat", description = "Chat with a specific agent")
    public Integer chat(@Parameters(index = "0", description = "The name or class of the agent") String name) {
        if (agentScannerSupplier == null || agentCreatorSupplier == null) {
            System.out.println(picocli.CommandLine.Help.Ansi.AUTO.string("@|bold,red Error: Agent services not initialized in CLI context.|@"));
            return 1;
        }

        List<Class<? extends Agent>> agents = agentScannerSupplier.apply("march");
        Agent selectedAgent = null;

        for (Class<? extends Agent> agentClass : agents) {
            try {
                Agent agent = agentCreatorSupplier.apply(agentClass, null);
                if (agent.getName().equalsIgnoreCase(name) || agentClass.getSimpleName().equalsIgnoreCase(name)) {
                    selectedAgent = agent;
                    break;
                }
            } catch (Exception e) {}
        }

        if (selectedAgent == null) {
            System.out.println(picocli.CommandLine.Help.Ansi.AUTO.string("@|bold,red Agent not found: " + name + "|@"));
            return 1;
        }

        System.out.println(picocli.CommandLine.Help.Ansi.AUTO.string("@|bold,cyan Starting chat with " + selectedAgent.getName() + "...|@"));
        System.out.println(picocli.CommandLine.Help.Ansi.AUTO.string("@|faint Type 'exit' to quit.|@"));
        
        String chatId = "cli-session-" + System.currentTimeMillis();
        selectedAgent.createNewChat(chatId);
        
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print(picocli.CommandLine.Help.Ansi.AUTO.string("@|bold,yellow > |@"));
            if (!scanner.hasNextLine()) break;
            String input = scanner.nextLine();
            if ("exit".equalsIgnoreCase(input.trim())) break;
            
            try {
                System.out.print(picocli.CommandLine.Help.Ansi.AUTO.string("@|italic,white Thinking...|@\r"));
                String response = selectedAgent.chat(input, chatId, parent.model);
                System.out.print("\r" + " ".repeat(20) + "\r"); // Clear thinking message
                System.out.println(picocli.CommandLine.Help.Ansi.AUTO.string("@|bold,blue " + selectedAgent.getName() + ":|@ " + response));
            } catch (Exception e) {
                System.out.println(picocli.CommandLine.Help.Ansi.AUTO.string("@|bold,red Error: " + e.getMessage() + "|@"));
            }
        }
        return 0;
    }

    @Override
    public Integer call() throws Exception {
        return 0;
    }
}
