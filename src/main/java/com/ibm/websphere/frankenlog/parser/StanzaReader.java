package com.ibm.websphere.frankenlog.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.text.ParsePosition;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;

class StanzaReader implements AutoCloseable {
    static final String DEFAULT_TIMESTAMP_FORMAT = "'['dd/MM/yy HH:mm:ss:SSS zzz']'";
    static final DateTimeFormatter DEFAULT_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_TIMESTAMP_FORMAT);

    final String filename;
    private final DateTimeFormatter formatter;
    private final BufferedReader in;
    private final List<String> lines;

    private TemporalAccessor previousTime = LocalDateTime.MIN;

    StanzaReader(String filename) throws IOException {
        this.filename = filename;
        this.formatter = DEFAULT_TIMESTAMP_FORMATTER;
        this.in = new BufferedReader(new FileReader(filename));
        this.lines = new ArrayList<>();
    }

    TemporalAccessor parseTime(String text) {
        return formatter.parse(text, new ParsePosition(0));
    }

    Stanza next() {
        try {
            // read in the nextLine line of trace
            String nextLine;

            // concatenate lines until the nextLine timestamp or the end of the stream
            while (null != (nextLine = in.readLine())) {
                try {
                    TemporalAccessor time = parseTime(nextLine);
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

