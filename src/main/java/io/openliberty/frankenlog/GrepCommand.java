package io.openliberty.frankenlog;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import static java.util.function.Predicate.not;
import static picocli.CommandLine.Help.Ansi.Style.faint;

@Command(name = "grep", description = "Search for lines with string patterns in your log file ")
public class GrepCommand implements Callable<Integer> {
    @ParentCommand
    private FrankenLog frankenLog;
    @ArgGroup
    private ContextOption contextOption;
    @Parameters(paramLabel = "pattern", description = "The regex pattern you want to find in the logfile")
    private Pattern pattern;
    @Parameters(
            paramLabel = "log file",
            converter = LogFile.Converter.class,
            description = "The logfiles you want to search through"
    )
    private LogFile logFile;

    GrepCommand() {
    }

    @Override
    public final Integer call() throws Exception {
        execute();
        return 0;
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
        default boolean peek(Stanza stanza) { return false; }
        default void match(Stanza stanza) {}
        default void printRemaining() {}

        static void print(Stanza stanza) {
            System.out.println(faint.on() + stanza + faint.off());
        }

        ContextPrinter NULL_BUFFER = new ContextPrinter() {};

        class BeforeContext implements ContextPrinter {
            final FifoFixedSizeQueue<Stanza> queue;

            public BeforeContext(int num) {
                //Store an extra stanza because peek is called before match, so there will be an extra stanza in queue that needs to be removed which is the matched stanza
                queue = new FifoFixedSizeQueue<>(num + 1);
            }

            public boolean peek(Stanza stanza) {
                queue.add(stanza);
                return false;
            }

            public void match(Stanza stanza) {
                queue.stream().filter(not(stanza::equals)).forEach(ContextPrinter::print);
                queue.clear();
            }
        }

        class AfterContext implements ContextPrinter {
            final int num;
            int linesToPrint;
            final List<Stanza> list;

            public AfterContext(int num) {
                this.num = num;
                list = new ArrayList<>(num);
            }

            public boolean peek(Stanza stanza) {
                if (linesToPrint > 0) {
                    list.add(stanza);
                    linesToPrint--;
                    return true;
                }
                list.forEach(ContextPrinter::print);
                list.clear();
                return false;
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
                if (!list.isEmpty()) {
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

            public boolean peek(Stanza stanza) {
                return ac.peek(stanza) || bc.peek(stanza);
            }

            public void match(Stanza stanza) {
                ac.match(stanza);
                bc.match(stanza);
            }

            public void printRemaining() { ac.printRemaining(); }
        }

        static ContextPrinter of(ContextOption option) {
            if (option == null) return NULL_BUFFER;
            if (option.before != null) return new ContextPrinter.BeforeContext(option.before);
            if (option.after != null) return new ContextPrinter.AfterContext(option.after);
            if (option.context != null) return new ContextPrinter.Context(option.context);
            return NULL_BUFFER;
        }
    }

    void execute() {
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
}
