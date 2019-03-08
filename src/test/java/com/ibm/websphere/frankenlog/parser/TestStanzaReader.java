package com.ibm.websphere.frankenlog.parser;

import test.model.LogFile;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestStanzaReader {
    @ParameterizedTest(name = "test StanzaReader with {0}")
    @EnumSource(LogFile.class)
    void testReadLogFile(LogFile logFile) throws IOException {
        try (StanzaReader reader = new StanzaReader(logFile.filename)) {
            for (String text: logFile.stanzaTexts) {
                assertThat(reader.next().text, is(text));
            }
            assertThat(reader.next(), is(nullValue()));
            assertThat(reader.next(), is(nullValue()));
        }
    }
}
