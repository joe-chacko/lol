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
import java.util.*;

public class Stanza implements AutoCloseable, Comparable<Stanza> {

    private final LogReader reader;
    private final String text;
    private String unformattedTime;
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

    public boolean isPreamble() {return Instant.MIN.equals(time);}

    public Stanza next() {return reader.next();}

    @Override
    public void close() {reader.close();}

    @Override
    public int compareTo(Stanza that) {return Comparator.comparing(Stanza::getTime).thenComparing(Stanza::getShortname).compare(this,that);}

    public String getText() {return text;}

    public String getShortname() {return reader.logFile.shortname;}

    public String getDisplayText(){
        return isPreamble() ?
                String.format("\n%s\n%s", reader.logFile.shortname, getText()) :
                reader.logFile.formatter.format(this.getTime()) + this.getText();
    }
    public Instant getTime() {return time;}

    public int getLines() {return lines;}

    public String getUnformattedTime() { return unformattedTime; }

}
