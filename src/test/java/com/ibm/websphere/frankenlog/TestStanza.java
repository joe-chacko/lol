package com.ibm.websphere.frankenlog;

import org.junit.Test;

import java.time.LocalDateTime;

import static com.ibm.websphere.frankenlog.TestStanzaReader.LINE_ONE_OF_TRACE;
import static com.ibm.websphere.frankenlog.TestStanzaReader.STANDARD_PREAMBLE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

public class TestStanza {
    @Test
    public void testOneLineOfTrace() throws Exception {
        try (Stanza firstStanza = Stanza.startReading("src/test/resources/one-line-of-trace.log")) {
            Stanza stanza = firstStanza;
            assertThat(stanza.text, is(STANDARD_PREAMBLE));
            assertThat(stanza.time, is(LocalDateTime.MIN));

            stanza = stanza.next();
            assertThat(stanza.text, is(LINE_ONE_OF_TRACE));
            assertThat(stanza.time, is(not(nullValue())));
            assertThat(stanza.time, is(not(LocalDateTime.MIN)));

            stanza = stanza.next();
            assertThat(stanza, is(nullValue()));
        }
    }
}
