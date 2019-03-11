package test.model;

import com.ibm.websphere.frankenlog.parser.Stanza;
import test.util.Streams;

import java.util.List;
import java.util.stream.Stream;

import static test.model.ExpectedStanza.BOGUS_BRACKET_TRACE;
import static test.model.ExpectedStanza.EMPTY_PREAMBLE;
import static test.model.ExpectedStanza.LINE_ONE_OF_TRACE;
import static test.model.ExpectedStanza.STANDARD_PREAMBLE;
import static test.util.Streams.toUnmodifiableList;

public enum ExpectedLog {
    EMPTY_LOG("src/test/resources/empty-trace.log"),
    ONE_LINE_LOG("src/test/resources/one-line-of-trace.log",
            STANDARD_PREAMBLE,
            LINE_ONE_OF_TRACE),
    NO_PREAMBLE_LOG("src/test/resources/no-preamble.log",
            LINE_ONE_OF_TRACE),
    PREAMBLE_ONLY_LOG("src/test/resources/preamble-only.log",
            STANDARD_PREAMBLE),
    BLANK_LINE_PREAMBLE_LOG("src/test/resources/blank-line-preamble.log",
            EMPTY_PREAMBLE,
            LINE_ONE_OF_TRACE),
    BOGUS_BRACKETS_LOG("src/test/resources/bogus-brackets-trace.log",
            STANDARD_PREAMBLE,
            LINE_ONE_OF_TRACE,
            BOGUS_BRACKET_TRACE)
    ;

    public final String filename;
    public final boolean hasPreamble;
    public final List<ExpectedStanza> stanzas;
    public final List<String> stanzaTimes;
    public final List<String> stanzaTexts;
    public final List<Integer> stanzaLines;
    public final List<Boolean> stanzaPreambles;
    public final int lines;

    ExpectedLog(String filename, ExpectedStanza...stanzas) {
        this.filename = filename;
        // if the first stanza has a negative time stamp, it is a pre-amble
        this.hasPreamble = Stream.of(stanzas).findFirst().map(s -> s.isPreamble).orElse(false);
        // create an immutable list of stanzas
        this.stanzas = Stream.of(stanzas).collect(toUnmodifiableList());
        // create an immutable list of stanza texts
        this.stanzaTexts = Stream.of(stanzas).map(s -> s.text).collect(toUnmodifiableList());
        // create an immutable list of stanza times
        this.stanzaTimes = Stream.of(stanzas).map(s -> s.time).collect(toUnmodifiableList());
        // create an immutable list of stanza line counts
        this.stanzaLines = Stream.of(stanzas).map(s -> s.lines).collect(toUnmodifiableList());
        // create an immutable list of booleans of whether the stanzas are preambles
        this.stanzaPreambles = Stream.of(stanzas).map(s -> s.isPreamble).collect(toUnmodifiableList());
        // add up the count of lines not including the preamble
        this.lines = Stream.of(stanzas).filter(s -> !s.isPreamble).mapToInt(s -> s.lines).sum();
    }

    public void verify(Stream<Stanza> actualStanzas) {
        Streams.zip(this.stanzas.stream(), actualStanzas, ExpectedStanza::verify);
    }
}
