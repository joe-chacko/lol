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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import test.model.ExpectedLog;
import test.model.ExpectedStanza;
import test.util.Streams;

import java.io.IOException;
import java.io.StringReader;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

import static io.openliberty.frankenlog.LogFile.getTimeStamp;
import static io.openliberty.frankenlog.LogFile.parseTime;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestStanza {
    @ParameterizedTest(name = "parse time for {0}")
    @EnumSource(ExpectedStanza.class)
    public void testParseTime(ExpectedStanza expectedStanza) {
        if (expectedStanza.isPreamble()) return;
        parseTime(expectedStanza.getText().split("\n")[0]);
    }

    @ParameterizedTest(name = "test Stanzas for {0}")
    @EnumSource(ExpectedLog.class)
    public void testStanzaNext(ExpectedLog expectedLog) throws Exception {
        try (Stanza firstStanza = new LogFile(expectedLog.getFilename()).next()) {
            expectedLog.verify(Streams.from(firstStanza, Stanza::next));
        }
    }

    @ParameterizedTest(name = "test Stanzas for {0}")
    @EnumSource(ExpectedLog.class)
    public void testStanzaToStream(ExpectedLog expectedLog) throws Exception {
        try (Stream<Stanza> stream = new LogFile(expectedLog.getFilename()).getStanzas()) {
            expectedLog.verify(stream);
        }
    }

    public static class TestReader {
        @ParameterizedTest(name = "test LogFile with {0}")
        @EnumSource(ExpectedLog.class)
        void testReadLogFile(ExpectedLog expectedLog) throws IOException {
            try (LogFile reader = new LogFile(expectedLog.getFilename())) {
                expectedLog.verify(Streams.from(reader::next));
            }
        }
    }

    @Test
    void testStanzaReader() {
        String[] lines = {
                "********************************************************************************",
                "product = Acme Foo Fighter",
                "********************************************************************************",
                "[10/17/22, 8:32:32:945 PDT] 00000001 id=00000000 com.acme.tracespec            I TRAS99999: The trace state has been changed",
                "[17/10/22 15:57:32:780 GMT] 00000001 id=00000000 SystemOut               I Some text: PDT"

        };
        String log = Stream.of(lines).collect(joining("\n"));
        assertThrows(DateTimeParseException.class, () -> parseTime(getTimeStamp(lines[0])));
        assertThrows(DateTimeParseException.class, () -> parseTime(getTimeStamp(lines[1])));
        assertThrows(DateTimeParseException.class, () -> parseTime(getTimeStamp(lines[2])));
        assertNotNull(parseTime(getTimeStamp(lines[3])));
        assertNotNull(parseTime(getTimeStamp(lines[4])));

        LogFile sr = new LogFile("trace.log", new StringReader(log));
        assertThat(sr.next().getLines(), is(3));
    }
}
