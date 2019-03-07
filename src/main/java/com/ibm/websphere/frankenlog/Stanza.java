package com.ibm.websphere.frankenlog;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Objects;

public class Stanza implements AutoCloseable, Comparable<Stanza> {

    public static Stanza startReading(String filename) throws IOException {
        return new StanzaReader(filename).next();
    }

    private final StanzaReader reader;
    public final String text;
    public final String source;
    public final LocalDateTime time;

    Stanza(StanzaReader reader, String text, TemporalAccessor time) {
        this.reader = reader;
        this.source = reader.filename;
        Objects.requireNonNull(text);
        this.text = text;
        this.time = LocalDateTime.from(time);
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
