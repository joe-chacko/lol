package com.ibm.websphere.frankenlog.parser;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import test.model.LogFile;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestStanza {
    @ParameterizedTest(name = "test Stanzas for {0}")
    @EnumSource(LogFile.class)
    public void testStanzasForLogFile(LogFile logFile) throws Exception {
        List<String> actualTimes = new ArrayList<>();
        List<String> actualTexts = new ArrayList<>();
        List<Integer> actualLines = new ArrayList<>();
        List<Boolean> actualPreambles = new ArrayList<>();
        try (Stanza firstStanza = Stanza.startReading(logFile.filename)) {
            for (Stanza stanza = firstStanza; stanza != null; stanza = stanza.next()) {
                actualTimes.add(stanza.time.atZone(ZoneId.of("UTC")).toString());
                actualTexts.add(stanza.text);
                actualLines.add(stanza.lines);
                actualPreambles.add(stanza.isPreamble());
            }
        }
        assertThat(actualTimes, is(logFile.stanzaTimes));
        assertThat(actualTexts, is(logFile.stanzaTexts));
        assertThat(actualLines, is(logFile.stanzaLines));
        assertThat(actualPreambles, is(logFile.stanzaPreambles));
    }
}
