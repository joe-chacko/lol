package io.openliberty.frankenlog;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.PropertiesDefaultProvider;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.openliberty.frankenlog.MergeUtil.merge;

@Command(
        name = "lol",
        mixinStandardHelpOptions = true,
        description = "Logs of Open Liberty - Unify concurrent logs into a coherent whole",
        version = "Logs of Open Liberty 1.0",
        subcommands = {HelpCommand.class, GrepCommand.class}, // other subcommands are annotated methods
        defaultValueProvider = PropertiesDefaultProvider.class)
public class Lol {
    public static void main(String... args) {
        Lol lol = new Lol();
        CommandLine commandLine = new CommandLine(lol);
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    @Command(name = "gather", description = "Unify and output concurrent logs")
    void gather(
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

    @Command(name = "gap", description = "Find the lines in you log with a time gap bigger than your inputted time (-t). If no time is entered then the lines with the biggest time gap will be returned")
    void gap(
            @Option(names = {"-t", "--time-gap"}, defaultValue = "-1", description = "The minimum seconds gap between two lines for them to be displayed")
            long timeGap,
            @Parameters(
                    paramLabel = "log file",
                    arity = "1",
                    converter = LogFile.Converter.class,
                    description = "The log file and the minimum time gap. If no time gap is provided then the lines with the biggest time gap will be returned"
            )
            LogFile lf) {
        if (timeGap == -1) {
            largestTimeGap(lf);
        } else {
            minimumTimeGap(lf, Duration.ofSeconds(timeGap));
        }
    }

    @Command(name = "grok", description = "Guess the inputted log's date format")
    void grok(
            @Parameters(
                    paramLabel = "files",
                    arity = "1..*",
                    converter = LogFile.Converter.class,
                    description = "The paths to the files you would like to guess the date format for"
            )
            List<LogFile> files) {
        files.forEach(this::grok);
    }

    private void grok(LogFile lf) {
        Stanza stanza = new LogReader(lf).getStanzas().filter(e -> !e.isPreamble()).findAny().orElse(null);
        System.out.println(stanza != null ?
                lf.filename + " -> " + LogFile.TimestampFormat.guessTimeStamp(stanza.getUnformattedTime()) :
                lf.filename + " -> No Timestamps found");
    }

    private void largestTimeGap(LogFile lf) {
        AtomicInteger ln = new AtomicInteger();
        AtomicReference<Duration> largestTimeGap = new AtomicReference<>(Duration.ofSeconds(0));
        AtomicReference<String> lines = new AtomicReference<>("Log file does not have two lines with timestamps");
        AtomicReference<Stanza> prevStanza = new AtomicReference<>();
        new LogReader(lf).getStanzas().forEach(st -> {
            Stanza prev = prevStanza.get();
            if (prev != null) {
                ln.addAndGet(prev.getText().split("\r\n|\r|\n").length);
                if (!prev.isPreamble()) {
                    Duration timeDiff = Duration.between(prev.getTime(), st.getTime());
                    if (timeDiff.abs().compareTo(largestTimeGap.get()) > 0) {
                        lines.set(String.format("Line %d: %s\nLine %d: %s", ln.get(), prev.getDisplayText(), ln.get() + 1, st.getDisplayText()));
                        largestTimeGap.set(timeDiff);
                    } else if (timeDiff.compareTo(largestTimeGap.get()) == 0) {
                        lines.set(String.format("%s\n\nLine %d: %s\nLine %d: %s", lines.get(), ln.get(), prev.getDisplayText(), ln.get() + 1, st.getDisplayText()));
                    }
                }
            }
            prevStanza.set(st);
        });
        if (lines.get().isEmpty()) {
            System.out.println("Log file does not have two lines with timestamps");
        } else {
            System.out.println(lines + "\n\nTime Gap = " + humanReadableFormat(largestTimeGap.get()));
        }
    }

    private void minimumTimeGap(LogFile lf, Duration minTimeGap) {
        AtomicInteger ln = new AtomicInteger();
        AtomicReference<Stanza> prevStanza = new AtomicReference<>();
        new LogReader(lf).getStanzas().forEach(st -> {
            Stanza prev = prevStanza.get();
            if (prev != null) {
                ln.addAndGet(prev.getText().split("\r\n|\r|\n").length); //Add the number lines in the stanza to the line number counter variable
                if (!prev.isPreamble()) {
                    Duration timeDiff = Duration.between(prev.getTime(), st.getTime());
                    if (timeDiff.abs().compareTo(minTimeGap) >= 0)
                        System.out.printf("Line %d: %s\nLine %d: %s\nTime Gap: %s\n\n", ln.get(), prev.getDisplayText(), ln.get() + 1, st.getDisplayText(), humanReadableFormat(timeDiff));
                }
            }
            prevStanza.set(st);
        });
    }

    String humanReadableFormat(Duration duration) {
        return duration.toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .toLowerCase();
    }
}