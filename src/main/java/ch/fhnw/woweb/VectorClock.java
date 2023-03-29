package ch.fhnw.woweb;

import java.util.Arrays;
import java.util.stream.Collectors;

public class VectorClock implements Comparable<VectorClock> {
    private final int[] counters;

    public VectorClock(int clientCount) {
        this.counters = new int[clientCount];
    }

    public VectorClock(int[] counters) {
        this.counters = counters;
    }

    public int[] getCounters() {
        return Arrays.copyOf(counters, counters.length);
    }

    public VectorClock increment(int ownedIndex) {
        assert(ownedIndex >= 0 && ownedIndex < counters.length);
        var newClock = new VectorClock(counters.length);
        System.arraycopy(counters, 0, newClock.counters, 0, counters.length);
        newClock.counters[ownedIndex] += 1;
        return newClock;
    }

    public VectorClock next(VectorClock other) {
        assert(counters.length == other.counters.length);
        var next = new VectorClock(counters.length);
        for (var i = 0; i < counters.length; i++) {
            next.counters[i] = Math.max(counters[i], other.counters[i]);
        }
        return next;
    }

    @Override
    public int compareTo(VectorClock other) {
        assert(counters.length == other.counters.length);
        for (var i = 0; i < counters.length; i++) {
            var diff = counters[i] - other.counters[i];
            if (diff != 0) {
                return diff;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        return "[" + Arrays.stream(counters)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(", ")) + "]";
    }
}
