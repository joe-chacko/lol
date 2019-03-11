package test.model;

import com.ibm.websphere.frankenlog.parser.Stanza;

import java.time.ZoneId;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public enum ExpectedStanza {
    EMPTY_PREAMBLE("-999999999-01-01T00:00Z[UTC]", ""),
    STANDARD_PREAMBLE("-999999999-01-01T00:00Z[UTC]",
            "********************************************************************************",
            "product = Acme Widget Wrangler",
            "trace.specification = *=info:logservice=detail",
            "********************************************************************************"),
    LINE_ONE_OF_TRACE("2019-03-01T15:57:32.780Z[UTC]",
            "[01/03/19 15:57:32:780 GMT] 00000001 id=00000000 TraceSpec               I TRAS0018I: blah blah"),

    BOGUS_BRACKET_TRACE("2019-03-01T15:57:32.780Z[UTC]",
            "[01/03/19 15:57:32:780 GMT] 00000001 id=00000000 SystemOut               I Some text:",
            "[22: hello, world]")
    ;
    public final String time;
    public final String text;
    public final int lines;
    public final boolean isPreamble;
    ExpectedStanza(String time, String...lines) {
        this.time = time;
        this.text = String.join("\n",lines);
        this.lines = lines.length;
        this.isPreamble = time.startsWith("-");
    }

    public void verify(Stanza actual) {
        assertThat("Time Zone should match expected time zone for " + this,
                actual.time.atZone(ZoneId.of("UTC")).toString(), is(this.time));
        assertThat("Text should match expected text for " + this,
                actual.text, is(this.text));
        assertThat("Line count should match expected line count for " + this,
                actual.lines, is(this.lines));
        assertThat("isPreamble should match expected value for " + this,
                actual.isPreamble(), is(this.isPreamble));
    }
}
