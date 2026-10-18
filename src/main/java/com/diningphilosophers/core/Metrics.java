package com.diningphilosophers.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates runtime statistics and fairness metrics from the Table.
 */
public class Metrics {
    private final long startTime;
    private long stopTime;
    private boolean deadlockOccurred = false;

    public Metrics() {
        this.startTime = System.currentTimeMillis();
        this.stopTime = 0;
    }

    public void markDeadlock() {
        this.deadlockOccurred = true;
    }

    public void markStopped() {
        if (this.stopTime == 0) {
            this.stopTime = System.currentTimeMillis();
        }
    }

    public long getStartTime() {
        return startTime;
    }

    public double getDurationSec() {
        long end = (stopTime > 0) ? stopTime : System.currentTimeMillis();
        return Math.max(0.0, (end - startTime) / 1000.0);
    }

    public boolean isDeadlockOccurred() {
        return deadlockOccurred;
    }

    public int getTotalMeals(List<Philosopher> philosophers) {
        return philosophers.stream().mapToInt(Philosopher::getMealsEaten).sum();
    }

    public Map<Integer, Integer> getMealsPerPhilosopher(List<Philosopher> philosophers) {
        Map<Integer, Integer> map = new HashMap<>();
        for (Philosopher p : philosophers) {
            map.put(p.getId(), p.getMealsEaten());
        }
        return Collections.unmodifiableMap(map);
    }

    public long getMaxWaitMs(List<Philosopher> philosophers) {
        return philosophers.stream()
                .mapToLong(Philosopher::getLongestWaitTimeMs)
                .max()
                .orElse(0L);
    }

    public int getFairnessMetric(List<Philosopher> philosophers) {
        if (philosophers.isEmpty()) return 0;
        int max = philosophers.stream().mapToInt(Philosopher::getMealsEaten).max().orElse(0);
        int min = philosophers.stream().mapToInt(Philosopher::getMealsEaten).min().orElse(0);
        return max - min; // Lower value indicates higher fairness (less variance)
    }
}
