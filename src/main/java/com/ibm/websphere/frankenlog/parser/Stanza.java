package com.ibm.websphere.frankenlog.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.text.ParsePosition;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
    private final String text;
    private final String source;
    private final Instant time;
    private final int lines;

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

    public String getText() {
        return text;
    }

    public String getSource() {
        return source;
    }

    public Instant getTime() {
        return time;
    }

    public int getLines() {
        return lines;
    }

    static class StanzaReader implements AutoCloseable {
        static final String DEFAULT_TIMESTAMP_FORMAT = "'['dd/MM/yy HH:mm:ss:SSS zzz']'";
        static final DateTimeFormatter DEFAULT_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_TIMESTAMP_FORMAT);

        final String filename;
        private final DateTimeFormatter formatter;
        private final BufferedReader in;
        private final List<String> lines;

        private Instant previousTime = Instant.MIN;

        StanzaReader(String filename) throws IOException {
            this.filename = filename;
            this.formatter = DEFAULT_TIMESTAMP_FORMATTER;
            this.in = new BufferedReader(new FileReader(filename));
            this.lines = new ArrayList<>();
        }

        Instant parseTime(String text) {
            return ZonedDateTime.from(formatter.parse(text, new ParsePosition(0))).toInstant();
        }

        Stanza next() {
            try {
                // read in the nextLine line of trace
                String nextLine;

                // concatenate lines until the nextLine timestamp or the end of the stream
                while (null != (nextLine = in.readLine())) {
                    try {
                        Instant time = parseTime(nextLine);
                        try {
                            if (!lines.isEmpty()) return createStanza();
                        } finally {
                            this.previousTime = time;
                        }
                    } catch (DateTimeParseException ignored) {
                        // there was no timestamp, so this is a continuation line
                    } finally {
                        lines.add(nextLine);
                    }
                }
                // at the end of the file - return a Stanza iff there is some content
                return lines.isEmpty() ? null : createStanza();
            } catch (IOException e) {
                throw new IOError(e);
            }
        }

        private Stanza createStanza() {
            try {
                return new Stanza(this, lines, this.previousTime);
            } finally {
                lines.clear();
            }
        }

        @Override
        public void close() {
            lines.clear();
            try {
                in.close();
            } catch(IOException e) {
                throw new IOError(e);
            }
        }
    }
}
