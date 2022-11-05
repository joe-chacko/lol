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
package test.model;

import io.openliberty.frankenlog.parser.Stanza;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public enum ExpectedStanza {
    EMPTY_PREAMBLE(null, ""),
    STANDARD_PREAMBLE(null,
            "********************************************************************************",
            "product = Acme Widget Wrangler",
            "trace.specification = *=info:logservice=detail",
            "********************************************************************************"),
    LINE_ONE_OF_TRACE("2019-03-01T15:57:32.780",
            "[01/03/19 15:57:32:780 GMT] 00000001 id=00000000 TraceSpec               I TRAS0018I: blah blah"),
    LINE_TWO_OF_TRACE("2019-03-01T15:57:33.550",
            "[01/03/19 15:57:33:550 GMT] 00000001 id=00000000 Other-Component         I XYZZY0001I: blah blah"),
    LINE_ONE_OF_TRACE_PST("2019-03-01T15:57:32.780",
            "[01/03/19 07:57:32:780 PST] 00000001 id=00000000 TraceSpec               I TRAS0018I: blah blah"),
    LINE_TWO_OF_TRACE_EST("2019-03-01T15:57:33.550",
            "[01/03/19 10:57:33:550 EST] 00000001 id=00000000 Other-Component         I XYZZY0001I: blah blah"),
    BOGUS_BRACKET_TRACE("2019-03-01T15:57:32.780",
            "[01/03/19 15:57:32:780 GMT] 00000001 id=00000000 SystemOut               I Some text:",
            "[22: hello, world]")
    ;
    private final boolean isPreamble;
    private final Instant time;
    private final String text;
    private final int lines;

    private ExpectedStanza(String time, String...lines) {
        this.isPreamble = time == null;
        this.time = isPreamble ? Instant.MIN : LocalDateTime.parse(time).toInstant(ZoneOffset.UTC);
        this.text = String.join("\n",lines);
        this.lines = lines.length;
    }

    public void verify(Stanza actual) {
        assertThat("Time Zone should match expected time zone for " + this,
                actual.getTime(), is(this.time));
        assertThat("Text should match expected text for " + this,
                actual.getText(), is(this.text));
        assertThat("Line count should match expected line count for " + this,
                actual.getLines(), is(this.lines));
        assertThat("isPreamble should match expected value for " + this,
                actual.isPreamble(), is(this.isPreamble));
    }

    public boolean isPreamble() {
        return isPreamble;
    }

    public Instant getTime() {
        return time;
    }

    public String getText() {
        return text;
    }

    public int getLines() {
        return lines;
    }
}
