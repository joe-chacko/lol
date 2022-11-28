package io.openliberty.frankenlog;

import picocli.CommandLine.ITypeConverter;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.text.ParsePosition;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LogFile {
    static class Converter implements ITypeConverter<LogFile> {
        static final Map<String,LogFile> CACHE = new HashMap<>();
        static final Pattern PATTERN = Pattern.compile("(.*):([YMD]{3})$");
        public LogFile convert(String arg) { return CACHE.computeIfAbsent(arg, this::create); }

        private LogFile create(String arg) {
            Matcher m = PATTERN.matcher(arg);
            if (m.find()) {
                String filename = m.group(1);
                TimestampFormat t = TimestampFormat.valueOf(m.group(2));
                return new LogFile(filename, t);
            }
            return new LogFile(arg);
        }
    }
    enum TimestampFormat {
        DMY("'['dd/MM/yy H:mm:ss:SSS zzz']'", Locale.UK),
        MDY("'['MM/dd/yy H:mm:ss:SSS zzz']'", Locale.US),
        YMD("'['yy/MM/dd H:mm:ss:SSS zzz']'", Locale.PRC),
        NONE("'['dd/MM/yy H:mm:ss:SSS zzz']'", Locale.UK) {
            Instant parse(String timestamp) {
                return guessTimeStamp(timestamp).parse(timestamp);
            }
        };

        static final Set<String> US_TIME_ZONES = Stream.of("AST", "EST", "EDT", "CST", "CDT", "MST", "MDT", "PST", "AST", "PDT", "AKST", "AKDT", "HST", "HAST", "HADT", "SST", "SDT", "CHST")
                .collect(Collectors.toUnmodifiableSet());
        private final DateTimeFormatter formatter;

        TimestampFormat(String format, Locale locale) {
            this.formatter = DateTimeFormatter.ofPattern(format, locale);
        }

        Instant parse(String timestamp) {
            var time = formatter.parse(timestamp.replaceAll(",", ""), new ParsePosition(0));
            var zdTime = ZonedDateTime.from(time);
            return zdTime.toInstant();
        }

        static TimestampFormat guessTimeStamp(String timeStamp) {
            boolean usDate = US_TIME_ZONES
                    .stream()
                    .anyMatch(timeStamp::contains);
            return usDate
                    ? TimestampFormat.MDY
                    : TimestampFormat.DMY;
        }

    }
    private static final AtomicInteger NEXT_CHAR = new AtomicInteger('A');
    final String filename;

    final String shortname = String.format("%c%1$c", NEXT_CHAR.getAndIncrement());

    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("'['yy/MM/dd H:mm:ss:SSS '" + shortname + "] '").withZone(ZoneOffset.UTC);

    final TimestampFormat format;

    LogFile(String filename, TimestampFormat tsf) {
        this.filename = filename;
        this.format = tsf;
    }

    LogFile(String filename) {
        this(filename, TimestampFormat.NONE);
    }

    Stream<Stanza> stream() {
        return new LogReader(this).getStanzas();
    }

    Reader getReader() {
        try {
            return new FileReader(filename);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
