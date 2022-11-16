package io.openliberty.frankenlog;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public enum MergeUtil {
    ;

    public static <T extends Comparable<T>> Stream<T> merge(Stream<T>... streams) {
        return merge(Stream.of(streams));
    }

    public static <T extends Comparable<T>> Stream<T> merge(Stream<Stream<T>> streamOfStreams) {
        return StreamSupport.stream(new MergingSpliterator<>(streamOfStreams), false);
    }

    /**
     * A spliterator that gets the lowest element from multiple streams
     */
    private static class MergingSpliterator<T extends Comparable<T>> implements Spliterator<T> {
        private final PriorityQueue<ComparableIterator<T>> pq;

        MergingSpliterator(Stream<Stream<T>> streamOfStreams) {
            this.pq = new PriorityQueue<>();
            // put all the non-empty streams into the priority queue as comparable iterators
            streamOfStreams.map(Stream::iterator).map(ComparableIterator::new).filter(Iterator::hasNext).forEach(pq::add);
        }

        public boolean tryAdvance(Consumer<? super T> action) {
            // The priority queue will return the lowest iterator first,
            // which is the iterator with the lowest T.
            var iterator = pq.poll();
            // If the pq was empty, there are no elements left.
            // Return false to indicate this.
            if (null == iterator) return false;
            // Getting the iterator from the queue means iter.hasNext() was true, so don't check again.
            // Calling next() will change the ordering of this iterator,
            // but that is ok because it is outside the priority queue
            action.accept(iterator.next());
            // Only re-insert the iterator if it has another element.
            // The ordering will potentially be different, but that is ok.
            if (iterator.hasNext()) pq.add(iterator);
            // Return true to show that the action was applied
            return true;
        }

        public Spliterator<T> trySplit() {
            return null;
        }

        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        public int characteristics() {
            return ORDERED | NONNULL;
        }
    }

    /**
     * An iterator whose natural ordering matches the natural ordering of its next element.
     * Note: the ordering of these objects can change as iteration progresses!
     * Also note: this class assumes that null is not a valid element in the underlying iterator.
     */
    private static class ComparableIterator<T extends Comparable<T>> implements Iterator<T>, Comparable<ComparableIterator<T>> {
        private final Iterator<T> iterator;
        private T nextElement;

        ComparableIterator(Iterator<T> iterator) {
            this.iterator = iterator;
            this.nextElement = iterator.hasNext() ? iterator.next() : null;
        }

        @Override
        public T next() {
            if (nextElement == null) throw new NoSuchElementException();
            T result = nextElement;
            this.nextElement = iterator.hasNext() ? iterator.next() : null;
            return result;
        }

        @Override
        public boolean hasNext() {
            return nextElement != null;
        }

        @Override
        public int compareTo(ComparableIterator<T> that) {
            if (null == this.nextElement) throw new IllegalStateException();
            if (null == that.nextElement) throw new IllegalStateException();
            return this.nextElement.compareTo(that.nextElement);
        }
    }
}
