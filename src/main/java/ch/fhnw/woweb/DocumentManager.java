package ch.fhnw.woweb;

import java.util.ArrayList;
import java.util.List;

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

    public VectorClock applyLocal(ChangeEvent event) {
        currentClock = currentClock.increment(ownedClockIndex);
        apply(currentClock, event);
        return currentClock;
    }

    public void applyExternal(ChangeEvent event, VectorClock eventClock) {
        currentClock = currentClock.increment(ownedClockIndex);
        apply(eventClock, event);
        currentClock = currentClock.next(eventClock);
    }

    private void apply(VectorClock eventClock, ChangeEvent event) {
        var change = new Change(eventClock, event);
        var i = computeInsertionIndex(eventClock);
        if (i == changes.size()) {
            event.apply(document);
            changes.add(change);
            return;
        }
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
