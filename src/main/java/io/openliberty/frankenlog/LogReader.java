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

class LogReader implements AutoCloseable {

    private final BufferedReader in;
    private final List<String> lines = new ArrayList<>();
    final LogFile logFile;
    private Instant previousTime = Instant.MIN;
    private String previousUnformattedTime = "";

    LogReader(LogFile lf) {
        this.logFile = lf;
        this.in = new BufferedReader(lf.getReader());
    }
    static String getTimeStamp(String text) {
        int closeBracketIndex = text.indexOf("] ");
        return text.substring(0, closeBracketIndex + 1);
    }




    Stanza next() {
        try {
            // read in the next line of trace
            String nextLine;

            // concatenate lines until the nextLine timestamp or the end of the stream
            while (null != (nextLine = in.readLine())) {
                try {
                    String timeStamp = getTimeStamp(nextLine);
                    Instant time = logFile.format.parse(timeStamp);
                    //If we get to here there was a time stamp and it is not the preamble or a continuation line
                    nextLine = nextLine.substring(timeStamp.length() + 1);
                    try {
                        if (!lines.isEmpty()) return createStanza();
                    } finally {
                        this.previousTime = time;
                        this.previousUnformattedTime = timeStamp;
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
            return new Stanza(this, lines, this.previousTime, this.previousUnformattedTime);
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
