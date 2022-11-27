package io.openliberty.frankenlog;

import picocli.CommandLine;
import picocli.CommandLine.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static io.openliberty.frankenlog.MergeUtil.merge;
import static java.util.function.Predicate.not;
import static picocli.CommandLine.Help.Ansi.Style.faint;

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

    @Command(name = "stuck", description = "Find the lines in you log with a time gap bigger than your inputted time (-t). If no time is entered then the lines with the biggest time gap will be returned")
    void stuck(
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

    static class ContextOption {
        @Option(names = {"-A", "--after-context"}, paramLabel = "num", description = "Print num log entries of trailing context after each match")
        Integer after;
        @Option(names = {"-B", "--before-context"}, paramLabel = "num", description = "Print num log entries of leading context before each match")
        Integer before;
        @Option(names = {"-C", "--context"}, paramLabel = "num", description = "Print num log entries of leading and trailing context surrounding each match")
        Integer context;
    }

    interface ContextPrinter {
        void peek(Stanza stanza);

        void match(Stanza stanza);

        void printRemaining();

        static void print(Stanza stanza) {
            System.out.println(faint.on() + stanza + faint.off());
        }

        ContextPrinter NULL_BUFFER = new ContextPrinter() {
            public void peek(Stanza stanza) {}
            public void match(Stanza stanza) {}
            public void printRemaining() {}


        };

        class BeforeContext implements ContextPrinter {
            final int num;
            final Queue<Stanza> queue = new LinkedList<>();

            public BeforeContext(int num) {
                this.num = num;
            }

            public void peek(Stanza stanza) {
                //Store an extra stanza because peek is called before match
                if (queue.size() > num) queue.remove();
                queue.add(stanza);
            }

            public void match(Stanza stanza) {
                queue.stream().filter(not(stanza::equals)).forEach(ContextPrinter::print);
                queue.clear();
            }

            public void printRemaining() {}
        }

        class AfterContext implements ContextPrinter {
            final int num;
            int linesToPrint;
            final List<Stanza> list;

            public AfterContext(int num) {
                this.num = num;
                list = new ArrayList<>(num);
            }

            public void peek(Stanza stanza) {
                if (linesToPrint > 0) {
                    list.add(stanza);
                    linesToPrint--;
                } else {
                    list.forEach(ContextPrinter::print);
                    list.clear();
                }
            }

            public void match(Stanza stanza) {
                //Flush any stored context
                list.stream().filter(not(stanza::equals)).forEach(ContextPrinter::print);
                //Record next num of stanzas to print
                list.clear();
                linesToPrint = num;
            }

            public void printRemaining() {
                //Used to print the remaining lines in the log fle after the last match if the remaining lines is less than user's inputted number
                if(!list.isEmpty()) {
                    list.forEach(ContextPrinter::print);
                    list.clear();
                }
            }
        }

        class Context implements ContextPrinter {
            AfterContext ac;
            BeforeContext bc;

            public Context(int num) {
                ac = new AfterContext(num);
                bc = new BeforeContext(num);
            }

            public void peek(Stanza stanza) {
                ac.peek(stanza);
                bc.peek(stanza);
            }

            public void match(Stanza stanza) {
                ac.match(stanza);
                bc.match(stanza);
            }

            public void printRemaining() {
                ac.printRemaining();
            }
        }

        static ContextPrinter of(ContextOption option) {
            if (option == null) return NULL_BUFFER;
            if (option.before != null) return new BeforeContext(option.before);
            if (option.after != null) return new AfterContext(option.after);
            if (option.context != null) return new Context(option.context);
            return NULL_BUFFER;
        }
    }

    @Command(name = "string", description = "Search for lines with string patterns in your log file ")
    void string(
            @ArgGroup final ContextOption contextOption,
            @Parameters(paramLabel = "pattern", description = "The regex pattern you want to find in the logfile")
            final Pattern pattern,
            @Parameters(
                    paramLabel = "log file",
                    converter = LogFile.Converter.class,
                    description = "The logfiles you want to search through"
            )
             final LogFile logFile) {
        ContextPrinter buffer = ContextPrinter.of(contextOption);
        logFile.stream()
                .filter(not(Stanza::isPreamble))
                .peek(buffer::peek)
                .filter(stanza -> stanza.matches(pattern))
                .peek(buffer::match)
                .map(stanza -> stanza.match(pattern))
                .forEach(System.out::println);
        buffer.printRemaining();
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
                        System.out.printf("Line %d: %s\nLine %d: %s\nTime Gap: %s\n%\n", ln.get(), prev.getDisplayText(), ln.get() + 1, st.getDisplayText(), humanReadableFormat(timeDiff));
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