package com.diningphilosophers.core;

import com.diningphilosophers.strategy.DiningStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the Dining Philosophers Table holding N philosophers and N shared forks
 * arranged in a circular topology.
 */
public class Table {
    private final int philosopherCount;
    private final List<Fork> forks;
    private final List<Philosopher> philosophers;
    private final EventBus eventBus;

    public Table(int philosopherCount, DiningStrategy strategy, EventBus eventBus, int thinkMs, int eatMs) {
        if (philosopherCount < 3 || philosopherCount > 8) {
            throw new IllegalArgumentException("Philosopher count must be between 3 and 8 inclusive.");
        }
        this.philosopherCount = philosopherCount;
        this.eventBus = eventBus;

        // 1. Initialize N shared forks
        this.forks = new ArrayList<>(philosopherCount);
        for (int i = 0; i < philosopherCount; i++) {
            forks.add(new Fork(i));
        }

        // 2. Initialize N philosophers positioned in a circle
        // Philosopher i has Left Fork = Fork i, Right Fork = Fork (i + 1) % N
        this.philosophers = new ArrayList<>(philosopherCount);
        for (int i = 0; i < philosopherCount; i++) {
            Fork leftFork = forks.get(i);
            Fork rightFork = forks.get((i + 1) % philosopherCount);
            Philosopher p = new Philosopher(i, leftFork, rightFork, strategy, eventBus);
            p.setThinkMs(thinkMs);
            p.setEatMs(eatMs);
            philosophers.add(p);
        }
    }

    public int getPhilosopherCount() {
        return philosopherCount;
    }

    public List<Fork> getForks() {
        return Collections.unmodifiableList(forks);
    }

    public List<Philosopher> getPhilosophers() {
        return Collections.unmodifiableList(philosophers);
    }

    public Philosopher getPhilosopher(int id) {
        return philosophers.get(id);
    }

    public Fork getFork(int id) {
        return forks.get(id);
    }

    public void updateStrategy(DiningStrategy strategy) {
        strategy.reset(philosopherCount);
        for (Philosopher p : philosophers) {
            p.setStrategy(strategy);
        }
    }

    public void updateTiming(int thinkMs, int eatMs) {
        for (Philosopher p : philosophers) {
            p.setThinkMs(thinkMs);
            p.setEatMs(eatMs);
        }
    }

    public void reset() {
        for (Philosopher p : philosophers) {
            p.reset();
        }
    }
}
