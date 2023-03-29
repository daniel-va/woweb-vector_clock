package ch.fhnw.woweb;

public class Main {
    private static final int CLIENT_COUNT = 2;
    private static final int CLIENT_ID = 0;

    private static final int EXTERNAL_CLIENT_ID = 1;

    private static DocumentManager manager;

    private static VectorClock externalClientClock = new VectorClock(CLIENT_COUNT);

    public static void main(String[] args) {
        manager = new DocumentManager(new VectorClock(CLIENT_COUNT), CLIENT_ID);

        // Simulate some local events.
        // As we are operating "locally", without any possible conflicts, we do not expect any special behavior here.
        //
        // That the events happen in the order that they were sent can be seen by checking their ID.
        // The first created node should have the ID 0, the second the ID 1, and so on.
        sendEvent(new ChangeEvent.CreateNode("Local User 1"));
        printDocument();

        sendEvent(new ChangeEvent.CreateNode("Local User 2"));
        printDocument();

        sendEvent(new ChangeEvent.UpdateNode(0, "Text of the first node."));
        printDocument();

        sendEvent(new ChangeEvent.UpdateNode(1, "Text of the second node."));
        printDocument();

        sendEvent(new ChangeEvent.MoveNode(0, 1));
        printDocument();

        // Simulate some external events - events that were caused by another client, and have been sent to us.
        // There should nothing special be happening here, as all clients are in-sync, without any chance for conflicts.
        receiveEvent(new ChangeEvent.CreateNode("External User 1"));
        receiveEvent(new ChangeEvent.UpdateNode(2, "Text of the first external node."));
        printDocument();

        // Mix some local and external events.
        // Again, this will not cause problems, as all clients sync their clocks before sending another event.
        sendEvent(new ChangeEvent.MoveNode(2, 0));
        receiveEvent(new ChangeEvent.MoveNode(2, 1));
        printDocument();

        // Now, we simulate a conflict by using `receiveDelayedEvent`.
        //
        // If everything were in order, the node #2 should be in position 1 after these events have been applied.
        // However, it is actually in position 0.
        //
        // This is due to `receiveDelayedEvent` simulating that the external client has not received the last
        // event happening before it (the move of #2 to position 0) and thus working with an outdated vector clock.
        // This causes the external client's move of #2 to position 1 to be prioritized below our local client's
        // last event, and it is thus overwritten.
        //
        // Note that this is indented and correct behavior:
        // The goal of vector clocks is not to guarantee that all events are sorted in the order that they have been executed.
        // Instead, it guarantees that all clients use the same order, as long as all of them have received all events.
        sendEvent(new ChangeEvent.MoveNode(2, 1));
        sendEvent(new ChangeEvent.MoveNode(2, 0));
        receiveDelayedEvent(new ChangeEvent.MoveNode(2, 1));
        printDocument();
    }

    /**
     * Simulates that the {@code event} was caused by our local client,
     * and that it will be sent to other clients.
     *
     * @param event - the event to be sent.
     */
    private static void sendEvent(ChangeEvent event) {
        // Apply the event to our local state.
        // The `eventClock` is the clock that we send to other clients, alongside the actual event.
        var eventClock = manager.applyLocal(event);

        // Update the clock of the external client as if it would have received our message.
        // This is what happens inside `DocumentManager` when calling `applyExternal`.
        externalClientClock = externalClientClock.increment(EXTERNAL_CLIENT_ID);
        externalClientClock = externalClientClock.next(eventClock);
    }

    /**
     * Simulates that the {@code event} was received from the external client,
     * and applies it to the {@link #manager}.
     *
     * @param event - the received event.
     */
    private static void receiveEvent(ChangeEvent event) {
        // Increment the external client's `VectorClock`.
        // This is what happens inside the `DocumentManager` when using `applyLocal`.
        //
        // `eventClock` is the `VectorClock` that the external client would send to us.
        var eventClock = externalClientClock.increment(EXTERNAL_CLIENT_ID);

        // Apply the external event to our local manager.
        manager.applyExternal(event, eventClock);
    }

    /**
     * This does the same as {@link #receiveEvent(ChangeEvent) receiveEvent},
     * but simulates that the event is out-of-sync with our local clock.
     *
     * <p>
     *     In detail, the clock used by the event is one behind our local counter.
     *     This means that the last event {@link #sendEvent(ChangeEvent) sent by our local client} will
     *     be prioritized over this event.
     * </p>
     *
     * @param event - the received event.
     */
    private static void receiveDelayedEvent(ChangeEvent event) {
        var counters = externalClientClock.getCounters();
        counters[CLIENT_ID] -= 1;
        manager.applyExternal(event, new VectorClock(counters));
    }

    /**
     * Prints the current state of the {@link #manager manager's document}.
     */
    private static void printDocument() {
        System.out.println(manager.getDocument());
        System.out.println();
    }
}