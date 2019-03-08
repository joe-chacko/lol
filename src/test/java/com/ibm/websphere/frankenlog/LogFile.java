package com.ibm.websphere.frankenlog;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

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
            "",
            Stanzas.LINE_ONE_OF_TRACE),
    BOGUS_BRACKETS_LOG("src/test/resources/bogus-brackets-trace.log",
            Stanzas.STANDARD_PREAMBLE,
            Stanzas.LINE_ONE_OF_TRACE,
            Stanzas.BOGUS_BRACKET_TRACE)

    ;

    private enum Stanzas {
        ;
        public static final String STANDARD_PREAMBLE = "" +
                "********************************************************************************\n" +
                "product = Acme Widget Wrangler\n" +
                "trace.specification = *=info:logservice=detail\n" +
                "********************************************************************************";
        public static final String LINE_ONE_OF_TRACE = "[01/03/19 15:57:32:780 GMT] 00000001 id=00000000 TraceSpec               I TRAS0018I: blah blah";

        public static final String BOGUS_BRACKET_TRACE = "[01/03/19 15:57:32:780 GMT] 00000001 id=00000000 SystemOut               I Some text:\n" +
                "[22: hello, world]";
    }

    public final String filename;
    public final List<String> stanzaText;

    LogFile(String filename, String...stanzaText) {
        this.filename = filename;
        this.stanzaText = unmodifiableList(asList(stanzaText));
    }
}
