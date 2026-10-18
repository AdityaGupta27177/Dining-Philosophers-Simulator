package com.diningphilosophers.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Thread-safe EventBus implementation (Plain Java, no Spring dependencies in core).
 * Dispatches simulation events to registered subscribers (such as SSE streams or loggers)
 * and keeps a bounded circular buffer of the most recent events for UI display.
 */
public class EventBus {
    private static final int MAX_HISTORY_SIZE = 200;
    private final List<Consumer<SimulationEvent>> listeners = new CopyOnWriteArrayList<>();
    private final LinkedList<SimulationEvent> eventHistory = new LinkedList<>();
    private final Object historyLock = new Object();

    public void subscribe(Consumer<SimulationEvent> listener) {
        listeners.add(listener);
    }

    public void unsubscribe(Consumer<SimulationEvent> listener) {
        listeners.remove(listener);
    }

    public void publish(SimulationEvent event) {
        // Record in bounded history
        synchronized (historyLock) {
            eventHistory.addLast(event);
            if (eventHistory.size() > MAX_HISTORY_SIZE) {
                eventHistory.removeFirst();
            }
        }

        // Notify all subscribers
        for (Consumer<SimulationEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                // Prevent one failing listener from blocking other subscribers
            }
        }
    }

    public List<SimulationEvent> getRecentEvents() {
        synchronized (historyLock) {
            return new ArrayList<>(eventHistory);
        }
    }

    public void clearHistory() {
        synchronized (historyLock) {
            eventHistory.clear();
        }
    }
}
