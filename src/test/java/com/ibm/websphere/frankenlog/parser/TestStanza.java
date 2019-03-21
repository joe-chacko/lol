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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import test.model.ExpectedLog;
import test.util.Streams;

import java.io.IOException;
import java.util.stream.Stream;

public class TestStanza {
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
}
