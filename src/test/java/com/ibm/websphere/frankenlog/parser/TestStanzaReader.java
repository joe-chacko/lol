package com.ibm.websphere.frankenlog.parser;

import com.ibm.websphere.frankenlog.LogFile;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class TestStanzaReader {
    @Parameters(name= "test{0}")
    public static LogFile[] data() {
        return LogFile.values();
    }

    public TestStanzaReader(LogFile logFile) {this.logFile = logFile;}

    private final LogFile logFile;

    @Test
    public void testReadLogFile() throws IOException {
        try (StanzaReader reader = new StanzaReader(logFile.filename)) {
            for (String text: logFile.stanzaText) {
                assertThat(reader.next().text, is(text));
            }
            assertThat(reader.next(), is(nullValue()));
            assertThat(reader.next(), is(nullValue()));
        }
    }
}
