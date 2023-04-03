package ch.fhnw.woweb;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code DocumentManager} represents and manages the local state of a document.
 * It stores all registered events, alongside their vector clocks, and is able to sort and apply them correctly.
 *
 * @see VectorClock in-depth explanation on how vector clocks are used to sort events.
 */
public class DocumentManager {
    private final Document document = new Document();
    private final List<Change> changes = new ArrayList<>();

    private VectorClock currentClock;
    private final int ownedClockIndex;

    public DocumentManager(VectorClock clock, int ownedClockIndex) {
        this.currentClock = clock;
        this.ownedClockIndex = ownedClockIndex;
    }

    public Document getDocument() {
        return new Document(document);
    }

    /**
     * Applies an event caused by the local client to the document.
     * @param event - The new event.
     * @return The vector clock associated with the event. Has to be distributed to other clients.
     */
    public VectorClock applyLocal(ChangeEvent event) {
        currentClock = currentClock.increment(ownedClockIndex);
        apply(currentClock, event);
        return currentClock;
    }

    /**
     * Applies an event caused by another client.
     * @param event - The new event.
     * @param eventClock - The vector clock associated with the new event.
     */
    public void applyExternal(ChangeEvent event, VectorClock eventClock) {
        currentClock = currentClock.increment(ownedClockIndex);
        apply(eventClock, event);
        currentClock = currentClock.next(eventClock);
    }

    private void apply(VectorClock eventClock, ChangeEvent event) {
        var change = new Change(eventClock, event);

        // Compute the index at which the event has to be inserted.
        var i = computeInsertionIndex(eventClock);

        // If the event's index is not in the list, it can simply be appended to it.
        // The event can then be applied without requiring a full rebuild of the document.
        if (i == changes.size()) {
            event.apply(document);
            changes.add(change);
            return;
        }

        // If the event is added somewhere between two existing events,
        // then we need to rebuild the entire document.
        changes.add(i, change);
        rebuildDocument();
    }

    private void rebuildDocument() {
        document.nodes.clear();
        for (var change : changes) {
            change.event.apply(document);
        }
    }

    private int computeInsertionIndex(VectorClock eventClock) {
        var i = changes.size() - 1;
        while (!isNextClock(eventClock, i)) {
            i -= 1;
        }
        return i + 1;
    }
    
    private boolean isNextClock(VectorClock nextClock, int prevChangeIndex) {
        assert prevChangeIndex < changes.size();
        if (prevChangeIndex < 0) {
            return true;
        }
        var prevChange = changes.get(prevChangeIndex);
        return nextClock.compareTo(prevChange.clock) >= 0;
    }

    private static final class Change {
        public final VectorClock clock;
        public final ChangeEvent event;

        private Change(VectorClock clock, ChangeEvent event) {
            this.clock = clock;
            this.event = event;
        }

        @Override
        public String toString() {
            return clock + " " + event;
        }
    }
}
