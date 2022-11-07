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
package com.ibm.websphere.frankenlog.parser;

import com.ibm.websphere.frankenlog.parser.Stanza.StanzaReader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import test.model.ExpectedLog;
import test.model.ExpectedStanza;
import test.util.Streams;

import java.io.IOException;
import java.rmi.StubNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestStanza {
    @ParameterizedTest(name = "parse time for {0}")
    @EnumSource(ExpectedStanza.class)
    public void testParseTime(ExpectedStanza expectedStanza) {
        if (expectedStanza.isPreamble()) return;
        StanzaReader.parseTime(expectedStanza.getText().split("\n")[0]);
    }

    @ParameterizedTest(name = "test Stanzas for {0}")
    @EnumSource(ExpectedLog.class)
    public void testStanzaStartReading(ExpectedLog expectedLog) throws Exception {
        try (Stanza firstStanza = Stanza.startReading(expectedLog.getFilename())) {
            expectedLog.verify(Streams.from(firstStanza, Stanza::next));
        }
    }

    @ParameterizedTest(name = "test Stanzas for {0}")
    @EnumSource(ExpectedLog.class)
    public void testStanzaToStream(ExpectedLog expectedLog) throws Exception {
        try (Stream<Stanza> stream = Stanza.toStream(expectedLog.getFilename())) {
            expectedLog.verify(stream);
        }
    }

    public static class TestReader {
        @ParameterizedTest(name = "test StanzaReader with {0}")
        @EnumSource(ExpectedLog.class)
        void testReadLogFile(ExpectedLog expectedLog) throws IOException {
            try (Stanza.StanzaReader reader = new Stanza.StanzaReader(expectedLog.getFilename())) {
                expectedLog.verify(Streams.from(reader::next));
            }
        }
    }

    @ParameterizedTest(name = "test Stanzas for {0}")
    @EnumSource(ExpectedLog.class)
    public void testFileStitching(ExpectedLog expectedLog) throws Exception {
        System.out.println("-----NEW TEST-----");
        try (Stream<Stanza> stream = Stanza.toStream(expectedLog.getFilename())) {
            stream.sorted(Comparator.comparing(Stanza::getTime))
                    .map(e -> "["+ e.getTime() + "]: " + e.getText())
                    .forEach(System.out::println);

//            expectedLog.verify(stream);
        }
    }
}
