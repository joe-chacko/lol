package io.openliberty.frankenlog;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.PropertiesDefaultProvider;

import java.io.File;
import java.util.List;

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
                    paramLabel = "logReaders",
                    arity = "1..*",
                    converter = LogFile.Converter.class,
                    description = "The paths to the files you would like to merge"
            )
            List<LogFile> logFiles) {
        logFiles.forEach(file -> System.out.println(file.shortname + " = " + file.filename));
        merge(logFiles.stream().map(LogReader::new).map(LogReader::getStanzas))
                .map(Stanza::getDisplayText)
                .forEach(System.out::println);
    }

    @Command(name = "stab", description = "Guess the inputted log's date format")
    void stab(
            @Parameters(
                    paramLabel = "files",
                    arity = "1..*",
                    converter = LogFile.Converter.class,
                    description = "The paths to the files you would like to guess the date format for"
            )
            List<LogFile> files) {
        files.forEach(this::stab);
    }

    private void stab(LogFile lf) {
        Stanza stanza = new LogReader(lf).getStanzas().filter(e-> !e.isPreamble()).findAny().orElse(null);
        System.out.println(stanza != null ?
                lf.filename + " -> " + LogFile.TimestampFormat.guessTimeStamp(stanza.getUnformattedTime()) :
                lf.filename + " -> No Timestamps found");
    }

}