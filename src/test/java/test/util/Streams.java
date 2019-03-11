package test.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

public enum Streams {
    ;
    public static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
        return java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(), Collections::unmodifiableList);
    }

    public static <T, U> void zip(Stream<T> stream1, Stream<U> stream2, BiConsumer<T, U> biConsumer) {
        Iterator<T> i1 = stream1.iterator();
        Iterator<U> i2 = stream2.iterator();
        while (i1.hasNext() || i2.hasNext()) {
            biConsumer.accept(i1.next(), i2.next());
        }
    }

    public static <T> Stream<T> from(Supplier<T> supplier) {
        return Iterators.asStream(Iterators.iterateOver(supplier));
    }

    public static <T> Stream<T> from(T first, Function<T,T> successor) {
        return Iterators.asStream(Iterators.iterateOver(first, successor));
    }
}
