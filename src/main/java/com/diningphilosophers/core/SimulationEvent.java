package com.diningphilosophers.core;

import java.time.Instant;

/**
 * Immutable event representing a point-in-time occurrence in the Dining Philosophers simulation.
 */
public class SimulationEvent {
    private final long timestamp;
    private final EventType type;
    private final Integer philosopherId;
    private final Integer forkId;
    private final String message;
    private final String details;

    public SimulationEvent(EventType type, Integer philosopherId, Integer forkId, String message, String details) {
        this.timestamp = Instant.now().toEpochMilli();
        this.type = type;
        this.philosopherId = philosopherId;
        this.forkId = forkId;
        this.message = message;
        this.details = details;
    }

    public SimulationEvent(EventType type, Integer philosopherId, String message) {
        this(type, philosopherId, null, message, null);
    }

    public SimulationEvent(EventType type, String message) {
        this(type, null, null, message, null);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public EventType getType() {
        return type;
    }

    public Integer getPhilosopherId() {
        return philosopherId;
    }

    public Integer getForkId() {
        return forkId;
    }

    public String getMessage() {
        return message;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return String.format("[%tT] %s - %s", timestamp, type, message);
    }
}
