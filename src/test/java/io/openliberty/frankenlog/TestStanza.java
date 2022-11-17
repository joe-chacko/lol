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
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static io.openliberty.frankenlog.LogFile.TimestampFormat.*;
import static io.openliberty.frankenlog.LogReader.getTimeStamp;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestStanza {
    @ParameterizedTest(name = "parse time for {0}")
    @EnumSource(ExpectedStanza.class)
    public void testParseTime(ExpectedStanza expectedStanza) {
        if (expectedStanza.isPreamble()) return;
        NONE.parse(expectedStanza.getText().split("\n")[0]);
    }

    @ParameterizedTest(name = "test Stanzas for {0}")
    @EnumSource(ExpectedLog.class)
    public void testStanzaNext(ExpectedLog expectedLog) throws Exception {
            try (Stanza firstStanza = new LogReader(new LogFile(expectedLog.getFilename())).next()) {
            expectedLog.verify(Streams.from(firstStanza, Stanza::next));
        }
    }

    @ParameterizedTest(name = "test Stanzas for {0}")
    @EnumSource(ExpectedLog.class)
    public void testStanzaToStream(ExpectedLog expectedLog) throws Exception {
        try (Stream<Stanza> stream = new LogReader(new LogFile(expectedLog.getFilename())).getStanzas()) {
            expectedLog.verify(stream);
        }
    }

    public static class TestReader {
        @ParameterizedTest(name = "test LogReader with {0}")
        @EnumSource(ExpectedLog.class)
        void testReadLogFile(ExpectedLog expectedLog) throws IOException {
            try (LogReader reader = new LogReader(new LogFile(expectedLog.getFilename()))) {
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
        assertThrows(DateTimeParseException.class, () -> NONE.parse(getTimeStamp(lines[0])));
        assertThrows(DateTimeParseException.class, () -> NONE.parse(getTimeStamp(lines[1])));
        assertThrows(DateTimeParseException.class, () -> NONE.parse(getTimeStamp(lines[2])));
        assertNotNull(NONE.parse(getTimeStamp(lines[3])));
        assertNotNull(MDY.parse(getTimeStamp(lines[3])));
        assertNotNull(NONE.parse(getTimeStamp(lines[4])));
        assertNotNull(DMY.parse(getTimeStamp(lines[4])));

        Pattern p = Pattern.compile("(.*):([YMD]{3})$");
        Pattern p2 = Pattern.compile(".");
        Matcher m = p.matcher("mylog.log:DMY");
        Matcher m2 = p2.matcher("HHhh22");
        if (m.find()) {
            System.out.println("TEST: " + m2.group());
            String filename = m.group(1);
            String t = m.group(2);
//            LogReader.TimestampFormat t = LogReader.TimestampFormat.valueOf(m.group(2));
            System.out.println("Filename = " + filename);
            System.out.println("TimeStamp = " + t);
        }
//
//        LogReader sr = new LogReader("trace.log", new StringReader(log));
//        assertThat(sr.next().getLines(), is(3));

    }
}
