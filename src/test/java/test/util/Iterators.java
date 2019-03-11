package test.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Iterators {
    public static <T> Iterator<T> iterateOver(T first, Function<T, T> successor) {
        return new Iterator<T>() {
            Optional<T> next = Optional.ofNullable(first);
            @Override
            public boolean hasNext() {
                return next.isPresent();
            }
            @Override
            public T next() {
                final Optional<T> current = next;
                next = next.map(successor);
                return current.orElseThrow(NoSuchElementException::new);
            }
        };
    }

    public static <T> Iterator<T> iterateOver(Supplier<T> supplier) {
        return iterateOver(supplier.get(), t -> supplier.get());
    }

    public static <T> Stream<T> asStream(Iterator<T> iterator) {
        Iterable<T> iterable = () -> iterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
