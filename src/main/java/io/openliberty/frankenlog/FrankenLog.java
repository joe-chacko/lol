package io.openliberty.frankenlog;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.PropertiesDefaultProvider;

import java.util.List;

@Command(
        name = "franken",
        mixinStandardHelpOptions = true,
        description = "FrankenLog - Unify concurrent logs into a coherent whole",
        version = "Frankenlog 1.0",
        subcommands = HelpCommand.class, // other subcommands are annotated methods
        defaultValueProvider = PropertiesDefaultProvider.class
)
public class FrankenLog {
    public static void main(String... args) {
        FrankenLog frankenlog = new FrankenLog();
        CommandLine commandLine = new CommandLine(frankenlog);
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    @Command(name = "stitch", description = "Unify and output concurrent logs"
    )
    void stitch(
            @Parameters(paramLabel = "numbers", arity = "1..*", description = "The numbers to add")
            List<String> numberInputs) {
        Integer sum = numberInputs.stream().mapToInt(Integer::valueOf).sum();
        System.out.println(sum);
    }
}
