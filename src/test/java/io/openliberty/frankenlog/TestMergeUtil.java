package io.openliberty.frankenlog;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static io.openliberty.frankenlog.MergeUtil.merge;
import static test.util.Streams.list;

public class TestMergeUtil {
    @Test
    void testSingleStream(){
        var stream = merge(Stream.of(1, 2, 3));
        Assertions.assertEquals(list(stream), List.of(1, 2, 3));
    }

    @Test
    void testTwoStreams1(){
        var stream = merge(Stream.of(1, 2, 3), Stream.of(4, 5, 6));
        Assertions.assertEquals(list(stream), List.of(1, 2, 3, 4, 5, 6));
    }

    @Test
    void testTwoStreams2(){
        var stream = merge(Stream.of(1, 3, 5, 7), Stream.of(2, 4, 6));
        Assertions.assertEquals(list(stream), List.of(1, 2, 3, 4, 5, 6, 7));
    }

    @Test
    void testTwoStreams3(){
        var stream = merge(Stream.of(1, 3, 7, 5), Stream.of(2, 4, 6));
        Assertions.assertEquals(list(stream), List.of(1, 2, 3, 4, 6, 7, 5));
    }

    @Test
    void testThreeStreams(){
        var stream = merge(Stream.of(0, 3, 6), Stream.of(2, 5, 8), Stream.of(1, 4, 7));
        Assertions.assertEquals(list(stream), List.of(0, 1, 2, 3, 4, 5, 6, 7, 8));
    }


}
