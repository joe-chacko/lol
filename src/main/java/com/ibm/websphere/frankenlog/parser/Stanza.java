package com.ibm.websphere.frankenlog.parser;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class Stanza implements AutoCloseable, Comparable<Stanza> {
    public static Stanza startReading(String filename) throws IOException {
        return new StanzaReader(filename).next();
    }

    public static Stream<Stanza> toStream (String filename) throws IOException {
        final StanzaReader reader = new StanzaReader(filename);
        return Stream
                .generate(reader::next)
                .takeWhile(Objects::nonNull)
                .onClose(reader::close);
    }

    private final StanzaReader reader;
    public final String text;
    public final String source;
    public final Instant time;
    public final int lines;

    /**
     * Create an immutable record from the provided data.
     * Parameters objects will .
     */
    Stanza(StanzaReader reader, List<String> text, Instant time) {
        this.reader = reader;
        this.source = reader.filename;
        Objects.requireNonNull(text);
        this.text = String.join("\n", text);
        this.time = time;
        this.lines = text.size();
    }

    public boolean isPreamble() {
        return Instant.MIN.equals(time);
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
