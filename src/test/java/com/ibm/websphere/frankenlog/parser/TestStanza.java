package com.ibm.websphere.frankenlog.parser;

import com.ibm.websphere.frankenlog.LogFile;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

public class TestStanza {

    @Test
    public void testOneLineOfTrace() throws Exception {
        final LogFile logFile = LogFile.ONE_LINE_LOG;
        try (Stanza firstStanza = Stanza.startReading(logFile.filename)) {
            Stanza stanza = firstStanza;
            assertThat(stanza.isPreamble(), is(true));
            String expectedText = logFile.stanzaText.get(0);
            final int expectedLineCount = expectedText.split("\n").length;
            assertThat(stanza.text, CoreMatchers.is(expectedText));
            assertThat(stanza.time, is(LocalDateTime.MIN));
            assertThat(stanza.lines, is(expectedLineCount));


            stanza = stanza.next();
            assertThat(stanza.isPreamble(), is(false));
//            assertThat(stanza.text, CoreMatchers.is(TestStanzaReader.LINE_ONE_OF_TRACE));
            assertThat(stanza.time, is(not(nullValue())));
            assertThat(stanza.time, is(not(LocalDateTime.MIN)));

            stanza = stanza.next();
            assertThat(stanza, is(nullValue()));
        }
    }
}
