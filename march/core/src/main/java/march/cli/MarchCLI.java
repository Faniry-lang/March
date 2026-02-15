package march.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "march",
        mixinStandardHelpOptions = true,
        version = "march-cli 1.0",
        description = "March Framework CLI",
        subcommands = {
                AgentCommand.class
        }
)
public class MarchCLI implements Runnable {

    @CommandLine.Option(names = {"-m", "--model"}, 
            description = "The LLM model to use", 
            defaultValue = "google/gemini-2.0-flash-001",
            scope = CommandLine.ScopeType.INHERIT)
    protected String model;

    @Override
    public void run() {
        System.out.println(CommandLine.Help.Ansi.AUTO.string("@|bold,cyan " +
                "  __  __                  _        _____ _      _____ \n" +
                " |  \\/  |                | |      / ____| |    |_   _|\n" +
                " | \\  / | __ _ _ __  ___| |__    | |    | |      | |  \n" +
                " | |\\/| |/ _` | '__|/ __| '_ \\   | |    | |      | |  \n" +
                " | |  | | (_| | |  | (__| | | |  | |____| |____ _| |_ \n" +
                " |_|  |_|\\__,_|_|   \\___|_| |_|   \\_____|______|_____|\n" +
                "|@"));
        
        System.out.println(CommandLine.Help.Ansi.AUTO.string("@|yellow March Framework CLI - v1.0.0|@"));
        System.out.println(CommandLine.Help.Ansi.AUTO.string("@|italic,white Type 'help' to see available commands.|@\n"));
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new MarchCLI()).execute(args);
        System.exit(exitCode);
    }
}
