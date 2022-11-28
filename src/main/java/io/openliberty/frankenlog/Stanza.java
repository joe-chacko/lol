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

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static picocli.CommandLine.Help.Ansi.Style.fg_red;

public class Stanza implements AutoCloseable, Comparable<Stanza> {

    private final LogReader reader;
    private final String text;
    private final String unformattedTime;
    private final Instant time;
    private final int lines;

    Stanza(LogReader reader, List<String> text, Instant time, String unformattedTime) {
        this.reader = reader;
        Objects.requireNonNull(text);
        this.text = String.join("\n", text);
        this.time = time;
        this.unformattedTime = unformattedTime;
        this.lines = text.size();
    }

    public boolean isPreamble() {
        return Instant.MIN.equals(time);
    }

    public Stanza next() {
        return reader.next();
    }

    @Override
    public void close() {
        reader.close();
    }

    @Override
    public int compareTo(Stanza that) {
        return Comparator.comparing(Stanza::getTime).thenComparing(Stanza::getShortname).compare(this, that);
    }

    public String getText() {
        return text;
    }

    public String getShortname() {
        return reader.logFile.shortname;
    }

    public String getDisplayText() {
        return isPreamble() ?
                String.format("\n%s\n%s", reader.logFile.shortname, getText()) :
                reader.logFile.formatter.format(this.getTime()) + this.getText();
    }

    public Instant getTime() {
        return time;
    }

    public int getLines() {
        return lines;
    }

    public String getUnformattedTime() {
        return unformattedTime;
    }

    boolean matches(Pattern pattern) {
        return pattern.matcher(text).find();
    }

    public CharSequence match(Pattern pattern) {
        StringBuilder s = new StringBuilder(unformattedTime).append(" ");
        //See if the regex pattern exists in this stanza
        Matcher m = pattern.matcher(text);
        //For each instance found of the pattern, append to stringbuilder "text" but replace the pattern with the same pattern but change the colour to red.
        //The text appended to s will stop at the end of the pattern matches found
        while (m.find()) m.appendReplacement(s, fg_red.on() + m.group() + fg_red.off());
        //Used to append the rest of "text" after all the matches found to the string builder
        m.appendTail(s);
        return s;
    }

    @Override
    public String toString() {
        return unformattedTime + " " + text;
    }
}