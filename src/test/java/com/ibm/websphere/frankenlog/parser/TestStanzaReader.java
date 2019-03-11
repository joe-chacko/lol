package com.ibm.websphere.frankenlog.parser;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import test.model.ExpectedLog;
import test.util.Streams;

import java.io.IOException;

public class TestStanzaReader {
    @ParameterizedTest(name = "test StanzaReader with {0}")
    @EnumSource(ExpectedLog.class)
    void testReadLogFile(ExpectedLog expectedLog) throws IOException {
        try (StanzaReader reader = new StanzaReader(expectedLog.filename)) {
            expectedLog.verify(Streams.from(reader::next));
        }
    }

}
