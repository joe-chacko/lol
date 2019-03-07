package com.ibm.websphere.frankenlog;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

public class TestStanzaReader {

    public static final String STANDARD_PREAMBLE = "" +
            "********************************************************************************\n" +
            "product = Acme Widget Wrangler\n" +
            "trace.specification = *=info:logservice=detail\n" +
            "********************************************************************************";
    public static final String LINE_ONE_OF_TRACE = "[01/03/19 15:57:32:780 GMT] 00000001 id=00000000 TraceSpec               I TRAS0018I: blah blah";

    @Test
    public void testEmptyTrace() throws Exception {
        try (StanzaReader reader = new StanzaReader("src/test/resources/empty-trace.log")) {
            assertThat(reader.next(), is(nullValue()));
            assertThat(reader.next(), is(nullValue()));
        }
    }

    @Test
    public void testPreambleOnly() throws Exception {
        try (StanzaReader reader = new StanzaReader("src/test/resources/preamble-only.log")) {
            assertThat(reader.next().text, is(STANDARD_PREAMBLE));
            assertThat(reader.next(), is(nullValue()));
            assertThat(reader.next(), is(nullValue()));
        }
    }

    @Test
    public void testBlankLinePreamble() throws Exception {
        try (StanzaReader reader = new StanzaReader("src/test/resources/blank-line-preamble.log")) {
            assertThat(reader.next().text, is(""));
            assertThat(reader.next().text, is(LINE_ONE_OF_TRACE));
            assertThat(reader.next(), is(nullValue()));
        }
    }

    @Test
    public void testOneLineOfTrace() throws Exception {
        try (StanzaReader reader = new StanzaReader("src/test/resources/one-line-of-trace.log")) {
            assertThat(reader.next().text, is(STANDARD_PREAMBLE));
            assertThat(reader.next().text, is(LINE_ONE_OF_TRACE));
            assertThat(reader.next(), is(nullValue()));
        }
    }

//    @Test
//    public void testBogusBracketsTrace() throws Exception {
//        try (StanzaReader reader = new StanzaReader("src/text/resources/bogus-brackets-trace.log")) {
//            assertThat(reader.nextLine(), is(STANDARD_PREAMBLE));
//            assertThat(reader.nextLine(), is(LINE_ONE_OF_TRACE));
//            assertThat(reader.nextLine(), is("" +
//                    "[01/03/19 15:57:32:780 GMT] 00000001 id=00000000 SystemOut               I Some text:\n" +
//                    "[22: hello, world]"));
//            assertThat(reader.nextLine(), is(nullValue()));
//        }
//    }

}
