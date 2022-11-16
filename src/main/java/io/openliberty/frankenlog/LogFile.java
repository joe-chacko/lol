/*
 * =============================================================================
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */

package io.openliberty.frankenlog;

import java.io.*;
import java.text.ParsePosition;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class LogFile implements AutoCloseable {
    static final String DEFAULT_TIMESTAMP_FORMAT = "'['dd/MM/yy H:mm:ss:SSS zzz']'";
    static final String US_TIMESTAMP_FORMAT = "'['MM/dd/yy H:mm:ss:SSS zzz']'";
    static final DateTimeFormatter DEFAULT_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_TIMESTAMP_FORMAT);
    static final DateTimeFormatter US_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(US_TIMESTAMP_FORMAT)
            //The locale will differ from once machine to another, so it's important to specify the locale to be used
            .withLocale(Locale.US);
    static final Set<String> US_TIME_ZONES = Stream.of("AST", "EST", "EDT", "CST", "CDT", "MST", "MDT", "PST", "AST", "PDT", "AKST", "AKDT", "HST", "HAST", "HADT", "SST", "SDT", "CHST")
            .collect(Collectors.toUnmodifiableSet());
    final String filename;
    private static final AtomicInteger NEXT_CHAR = new AtomicInteger('A');
    final String shortname = String.format("%c%1$c", NEXT_CHAR.getAndIncrement());
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("'['yy/MM/dd H:mm:ss:SSS '" + shortname + "] '").withZone(ZoneOffset.UTC);

    private final BufferedReader in;
    private final List<String> lines = new ArrayList<>();

    private Instant previousTime = Instant.MIN;

    LogFile(String filename, Reader rdr) {
        this.filename = filename;
        this.in = new BufferedReader(rdr);
    }

    LogFile(String filename) {
        this(filename, getReader(filename));
    }

    private static Reader getReader(String filename) {
        try {
            return new FileReader(filename);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    LogFile(File file) {
        this(file.getName());
    }

    static String getTimeStamp(String text) {
        int closeBracketIndex = text.indexOf("] ");
        return text.substring(0, closeBracketIndex + 1);
    }

    static Instant parseTime(String timeStamp) {
        boolean usDate = US_TIME_ZONES
                .stream()
                .anyMatch(timeStamp::contains);
        DateTimeFormatter TIMESTAMP_FORMATTER = usDate
                ? US_TIMESTAMP_FORMATTER
                : DEFAULT_TIMESTAMP_FORMATTER;
        return ZonedDateTime.from(TIMESTAMP_FORMATTER.parse(timeStamp.replaceAll(",", ""), new ParsePosition(0))).toInstant();
    }

    Stanza next() {
        try {
            // read in the next line of trace
            String nextLine;

            // concatenate lines until the nextLine timestamp or the end of the stream
            while (null != (nextLine = in.readLine())) {
                try {
                    String timeStamp = getTimeStamp(nextLine);
                    Instant time = parseTime(timeStamp);
                    //If we get to here there was a time stamp and it is not the preamble or a continuation line
                    nextLine = nextLine.substring(timeStamp.length() + 1);
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
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    Stream<Stanza> getStanzas() {
        return Stream.generate(this::next).takeWhile(Objects::nonNull).onClose(this::close);
    }
}
