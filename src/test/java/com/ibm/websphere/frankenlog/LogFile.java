package com.ibm.websphere.frankenlog;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum LogFile {
    EMPTY_LOG("src/test/resources/empty-trace.log"),
    ONE_LINE_LOG("src/test/resources/one-line-of-trace.log",
            Stanzas.STANDARD_PREAMBLE,
            Stanzas.LINE_ONE_OF_TRACE),
    NO_PREAMBLE_LOG("src/test/resources/no-preamble.log",
            Stanzas.LINE_ONE_OF_TRACE),
    PREAMBLE_ONLY_LOG("src/test/resources/preamble-only.log",
            Stanzas.STANDARD_PREAMBLE),
    BLANK_LINE_PREAMBLE_LOG("src/test/resources/blank-line-preamble.log",
            Stanzas.EMPTY_PREAMBLE,
            Stanzas.LINE_ONE_OF_TRACE),
    BOGUS_BRACKETS_LOG("src/test/resources/bogus-brackets-trace.log",
            Stanzas.STANDARD_PREAMBLE,
            Stanzas.LINE_ONE_OF_TRACE,
            Stanzas.BOGUS_BRACKET_TRACE)
    ;

    private enum Stanzas {
        EMPTY_PREAMBLE("", ""),
        STANDARD_PREAMBLE("",
                "********************************************************************************",
                "product = Acme Widget Wrangler",
                "trace.specification = *=info:logservice=detail",
                "********************************************************************************"),
        LINE_ONE_OF_TRACE("",
                "[01/03/19 15:57:32:780 GMT] 00000001 id=00000000 TraceSpec               I TRAS0018I: blah blah"),

        BOGUS_BRACKET_TRACE("",
                "[01/03/19 15:57:32:780 GMT] 00000001 id=00000000 SystemOut               I Some text:",
                "[22: hello, world]")
        ;
        private final String time;
        private final String text;
        private final int lines;
        Stanzas(String time, String...lines) {
            this.time = time;
            this.text = String.join("\n",lines);
            this.lines = lines.length;

        }
    }

    public final String filename;
    public final List<String> stanzaText;
    public final int lines;

    LogFile(String filename, Stanzas...stanzas) {
        this.filename = filename;
        this.stanzaText = Stream.of(stanzas).map(s -> s.text).collect(Collectors.toList());
        this.lines = Stream.of(stanzas).filter(s -> !s.time.startsWith("-")).mapToInt(s -> s.lines).sum();
    }
}
