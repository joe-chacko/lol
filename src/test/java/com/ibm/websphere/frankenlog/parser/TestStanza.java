package com.ibm.websphere.frankenlog.parser;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import test.model.ExpectedLog;
import test.util.Streams;

import java.util.stream.Stream;

public class TestStanza {
    @ParameterizedTest(name = "test Stanzas for {0}")
    @EnumSource(ExpectedLog.class)
    public void testStanzaStartReading(ExpectedLog expectedLog) throws Exception {
        try (Stanza firstStanza = Stanza.startReading(expectedLog.filename)) {
            expectedLog.verify(Streams.from(firstStanza, Stanza::next));
        }
    }

    @ParameterizedTest(name = "test Stanzas for {0}")
    @EnumSource(ExpectedLog.class)
    public void testStanzaToStream(ExpectedLog expectedLog) throws Exception {
        try (Stream<Stanza> stream = Stanza.toStream(expectedLog.filename)) {
            expectedLog.verify(stream);
        }
    }
}
