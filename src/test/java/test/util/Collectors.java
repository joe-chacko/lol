package test.util;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collector;

public enum Collectors {
    ;
    public static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
        return java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(), Collections::unmodifiableList);
    }
}
