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

import picocli.CommandLine;

import java.io.*;
import java.sql.Time;
import java.text.ParsePosition;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class LogFile implements AutoCloseable {
    static class Converter implements CommandLine.ITypeConverter<LogFile> {
        public LogFile convert(String value) throws Exception {
            Pattern p = Pattern.compile("(.*):([YMD]{3})$");
            Matcher m = p.matcher(value);
            if (m.find()) {
                String filename = m.group(1);
                TimestampFormat t = TimestampFormat.valueOf(m.group(2));
                return new LogFile(filename, t);
            }
            return new LogFile(value);
        }
    }

    ;
    enum TimestampFormat{
        DMY("'['dd/MM/yy H:mm:ss:SSS zzz']'", Locale.UK),
        MDY("'['MM/dd/yy H:mm:ss:SSS zzz']'", Locale.US),
        YMD("'['yy/MM/dd H:mm:ss:SSS zzz']'", Locale.PRC),
        NON("'['dd/MM/yy H:mm:ss:SSS zzz']'", Locale.UK);
        private final DateTimeFormatter formatter;

        TimestampFormat(String format, Locale locale) {
            this.formatter = DateTimeFormatter.ofPattern(format, locale);
        }

        Instant parse(String timestamp) {
            var time = formatter.parse(timestamp.replaceAll(",", ""), new ParsePosition(0));
            var zdTime = ZonedDateTime.from(time);
            return zdTime.toInstant();
        }
    }
    static final Set<String> US_TIME_ZONES = Stream.of("AST", "EST", "EDT", "CST", "CDT", "MST", "MDT", "PST", "AST", "PDT", "AKST", "AKDT", "HST", "HAST", "HADT", "SST", "SDT", "CHST")
            .collect(Collectors.toUnmodifiableSet());
    final String filename;
    private static final AtomicInteger NEXT_CHAR = new AtomicInteger('A');
    final String shortname = String.format("%c%1$c", NEXT_CHAR.getAndIncrement());
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("'['yy/MM/dd H:mm:ss:SSS '" + shortname + "] '").withZone(ZoneOffset.UTC);

    final TimestampFormat format;
    private final BufferedReader in;
    private final List<String> lines = new ArrayList<>();

    private Instant previousTime = Instant.MIN;

    LogFile(String filename, TimestampFormat tsf, Reader rdr) {
        this.filename = filename;
        this.format = tsf;
        this.in = new BufferedReader(rdr);
    }

    LogFile(String filename, TimestampFormat tsf) {
        this(filename, tsf, getReader(filename));
    }

    LogFile(String filename){
        this(filename, TimestampFormat.NON);
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

    static Instant parseTime(String timeStamp, TimestampFormat tsf) {
        return tsf.parse(timeStamp);
    }

    static Instant parseTime(String timeStamp) {
        boolean usDate = US_TIME_ZONES
                .stream()
                .anyMatch(timeStamp::contains);
        TimestampFormat tsf = usDate
                ? TimestampFormat.MDY
                : TimestampFormat.DMY;
        return tsf.parse(timeStamp);
    }


    Stanza next() {
        try {
            // read in the next line of trace
            String nextLine;

            // concatenate lines until the nextLine timestamp or the end of the stream
            while (null != (nextLine = in.readLine())) {
                try {
                    String timeStamp = getTimeStamp(nextLine);
                    Instant time = format == TimestampFormat.NON ? parseTime(timeStamp) : parseTime(timeStamp, format);
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
