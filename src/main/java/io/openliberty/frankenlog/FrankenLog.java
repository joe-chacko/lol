package io.openliberty.frankenlog;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.PropertiesDefaultProvider;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.openliberty.frankenlog.MergeUtil.merge;

@Command(name = "franken", mixinStandardHelpOptions = true, description = "FrankenLog - Unify concurrent logs into a coherent whole", version = "Frankenlog 1.0", subcommands = HelpCommand.class, // other subcommands are annotated methods
        defaultValueProvider = PropertiesDefaultProvider.class)
public class FrankenLog {
    public static void main(String... args) {
        FrankenLog frankenlog = new FrankenLog();
        CommandLine commandLine = new CommandLine(frankenlog);
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    @Command(name = "stitch", description = "Unify and output concurrent logs")
    void stitch(
            @Parameters(
                    paramLabel = "logFiles",
                    arity = "1..*",
                    converter = LogFile.Converter.class,
                    description = "The paths to the files you would like to merge"
            )
            List<LogFile> logFiles) {
        logFiles.forEach(file -> System.out.println(file.shortname + " = " + file.filename));
        merge(logFiles.stream().map(LogFile::getStanzas))
                .map(Stanza::getDisplayText)
                .forEach(System.out::println);
    }

    @Command(name = "stab", description = "Guess the inputted log's date format")
    void stab(@Parameters(paramLabel = "files", arity = "1..*", description = "The paths to the files you would like to guess the date format for") List<File> files) {
        files.forEach(this::stab);
    }

    private void stab(File file) {
        LogFile lf = new LogFile(file);
        Stanza stanza = lf.getStanzas().filter(e-> !e.isPreamble()).findAny().orElse(null);
        System.out.println(stanza != null ?
                file.getName() + " -> " + LogFile.guessTimeStamp(stanza.getUnformattedTime()) :
                file.getName() + " -> No Timestamps found");
    }

}