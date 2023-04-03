package ch.fhnw.woweb;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * A vector clock allows for ordering of data generated asynchronously in a distributed system.
 * Vector clocks are sorted in the same way by every client in that system,
 * without requiring any other data exchange outside of normal event synchronization.
 *
 * <p>
 *    This implementation consists of an array,
 *    where every index contains the counter of a specific client.
 *    We refer to a client's index as its "ID".
 *    This means that each index has to be mapped to exactly one single client.
 * </p>
 *
 * <h1>How it works</h1>
 * <p>
 *     Every client holds a local vector clock value.
 *     Each such clock starts with all counters set to 0.
 * </p>
 * <p>
 *     When a client causes an event (e.g. creates a new paragraph, moves a paragraph, or similar),
 *     it executes the following steps:
 *     <ol>
 *         <li>
 *             It increases its own vector clock counter by one.
 *         </li>
 *         <li>
 *             It stores the new event, alongside a copy of the local vector clock value.
 *         </li>
 *         <li>
 *             It distributes the new event to the other clients in the system,
 *             alongside a copy of its local vector clock value.
 *         </li>
 *     </ol>
 * </p>
 * <p>
 *     When a client receives an event from another client in the system,
 *     it executes the following steps:
 *     <ol>
 *         <li>
 *             It increases its own vector clock counter by one.
 *         </li>
 *         <li>
 *             It stores the new event, alongside a copy of the local vector clock value.
 *         </li>
 *         <lI>
 *             It replaces its local vector clock with a new value.
 *             That new value is made up of the maximum counters of both the previous local vector clock
 *             and the event's own vector clock.
 *         </lI>
 *     </ol>
 * </p>
 * <p>
 *     As long as all participating clients in the system follow the process as detailed above,
 *     then each client will hold a local list of all events, where each event is associated with a unique vector clock value.
 *     These vector clock values can then be used so sort that list. The resulting order of events will be the same for all clients.
 * </p>
 * <p>
 *     Vector clocks are compared as follows:
 *     <ul>
 *         <li>
 *             For two vector clocks {@code a} and {@code b}, a is greater than {@code b} if and only if
 *             every element of {@code a} is greater than or equal to the element at the same index in {@code b},
 *             with at least one element of {@code a} needing to be greater, not equal.
 *         </li>
 *     </ul>
 *     This definition only allows for a <i>partial ordering</i>, i.e. there are vector clock combinations where
 *     we can't determine if either {@code a} or {@code b} is greater, only that they are or are not equal.
 *     Such combinations are those where at least one element of {@code a} is greater than its corresponding element
 *     in {@code b}, while there is also at least one element of {@code a} that is smaller than its corresponding element
 *     in {@code b}. A simple example of two vector clocks matching this would be {@code [1 0] <=> [0 1]}
 * </p>
 * <p>
 *     To allow for <i>total ordering</i> of vector clocks, we need to define rules for <b>conflict resolution</b>.
 *     These rules are applied every time that the partial ordering fails to determine the order of two clocks.
 *     In this implementation, we simply assign priorities to the IDs (i.e. indices) of the vector clock.
 *     A smaller ID has a greater priority, which means that the previous conflicting example of {@code [1 0] <=> [0 1]}
 *     would simply result in {@code [1 0] > [0 1]}, as the index 0 has the highest priority.
 * </p>
 */
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
