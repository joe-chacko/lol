package com.ibm.websphere.frankenlog.parser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class Stanza implements AutoCloseable, Comparable<Stanza> {
    public static Stanza startReading(String filename) throws IOException {
        return new StanzaReader(filename).next();
    }

    private final StanzaReader reader;
    public final String text;
    public final String source;
    public final LocalDateTime time;
    public final int lines;

    /**
     * Create an immutable record from the provided data.
     * Parameters will not be modified.
     */
    Stanza(StanzaReader reader, List<String> text, TemporalAccessor time) {
        this.reader = reader;
        this.source = reader.filename;
        Objects.requireNonNull(text);
        this.text = String.join("\n", text);
        this.time = LocalDateTime.from(time);
        this.lines = text.size();
    }

    public boolean isPreamble() {
        return time == LocalDateTime.MIN;
    }

    public Stanza next() {
        return reader.next();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Override
    public int compareTo(Stanza that) {
        return this.time.compareTo(that.time);
    }
}
