package test.model;

import com.ibm.websphere.frankenlog.parser.Stanza;
import test.util.Streams;

import java.io.File;
import java.io.FileWriter;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Stream;

import static test.model.ExpectedStanza.BOGUS_BRACKET_TRACE;
import static test.model.ExpectedStanza.EMPTY_PREAMBLE;
import static test.model.ExpectedStanza.LINE_ONE_OF_TRACE;
import static test.model.ExpectedStanza.LINE_ONE_OF_TRACE_PST;
import static test.model.ExpectedStanza.LINE_TWO_OF_TRACE;
import static test.model.ExpectedStanza.LINE_TWO_OF_TRACE_EST;
import static test.model.ExpectedStanza.STANDARD_PREAMBLE;
import static test.util.Streams.toUnmodifiableList;

public enum ExpectedLog {
    EMPTY_LOG(),
    ONE_LINE_LOG(
            STANDARD_PREAMBLE,
            LINE_ONE_OF_TRACE),
    NO_PREAMBLE_LOG(
            LINE_ONE_OF_TRACE),
    PREAMBLE_ONLY_LOG(
            STANDARD_PREAMBLE),
    BLANK_LINE_PREAMBLE_LOG(
            EMPTY_PREAMBLE,
            LINE_ONE_OF_TRACE),
    TWO_LINE_LOG(
            STANDARD_PREAMBLE,
            LINE_ONE_OF_TRACE,
            LINE_TWO_OF_TRACE),
    BOGUS_BRACKETS_LOG(
            STANDARD_PREAMBLE,
            LINE_ONE_OF_TRACE,
            BOGUS_BRACKET_TRACE),
    MIXED_ZONE_LOG(
            STANDARD_PREAMBLE,
            LINE_ONE_OF_TRACE_PST,
            LINE_TWO_OF_TRACE_EST,
            BOGUS_BRACKET_TRACE),
    ;

    private final String filename;
    private final boolean hasPreamble;
    private final List<ExpectedStanza> stanzas;
    private final int lines;

    ExpectedLog(ExpectedStanza... stanzas) {
        // if the first stanza has a negative time stamp, it is a pre-amble
        this.hasPreamble = Stream.of(stanzas).findFirst().map(ExpectedStanza::isPreamble).orElse(false);
        // create an immutable list of stanzas
        this.stanzas = Stream.of(stanzas).collect(toUnmodifiableList());
        // add up the count of lines not including the preamble
        this.lines = Stream.of(stanzas).filter(ExpectedStanza::isPreamble).mapToInt(ExpectedStanza::getLines).sum();
        this.filename = createLogFile(name(), stanzas);
    }

    private static String createLogFile(String name, ExpectedStanza...stanzas) {
        try {
            File dir = new File("build/resources/test");
            dir.mkdirs();
            assert dir.exists();
            assert dir.isDirectory();
            File tmpFile = File.createTempFile(name, ".log", dir);
            try (PrintWriter out = new PrintWriter(new FileWriter(tmpFile, false))) {
                Stream.of(stanzas).map(ExpectedStanza::getText).forEachOrdered(out::println);
            }
            return tmpFile.getPath();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public void verify(Stream<Stanza> actualStanzas) {
        Streams.zip(this.stanzas.stream(), actualStanzas, ExpectedStanza::verify);
    }

    public String getFilename() {
        return filename;
    }

    public boolean hasPreamble() {
        return hasPreamble;
    }

    public List<ExpectedStanza> getStanzas() {
        return stanzas;
    }

    public int getLines() {
        return lines;
    }
}
