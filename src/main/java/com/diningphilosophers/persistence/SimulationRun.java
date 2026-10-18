package com.diningphilosophers.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity storing historical simulation run records in the H2 file-backed database.
 */
@Entity
@Table(name = "simulation_runs")
public class SimulationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String strategy;

    @Column(nullable = false)
    private int philosopherCount;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private double durationSec;

    private boolean deadlockOccurred;

    private int totalMeals;

    @Column(length = 2000)
    private String mealsPerPhilosopher; // JSON format

    private long maxWaitMs;

    private int fairnessMetric;

    public SimulationRun() {}

    public SimulationRun(String strategy, int philosopherCount, LocalDateTime startTime,
                         double durationSec, boolean deadlockOccurred, int totalMeals,
                         String mealsPerPhilosopher, long maxWaitMs, int fairnessMetric) {
        this.strategy = strategy;
        this.philosopherCount = philosopherCount;
        this.startTime = startTime;
        this.durationSec = durationSec;
        this.deadlockOccurred = deadlockOccurred;
        this.totalMeals = totalMeals;
        this.mealsPerPhilosopher = mealsPerPhilosopher;
        this.maxWaitMs = maxWaitMs;
        this.fairnessMetric = fairnessMetric;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public int getPhilosopherCount() { return philosopherCount; }
    public void setPhilosopherCount(int philosopherCount) { this.philosopherCount = philosopherCount; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public double getDurationSec() { return durationSec; }
    public void setDurationSec(double durationSec) { this.durationSec = durationSec; }

    public boolean isDeadlockOccurred() { return deadlockOccurred; }
    public void setDeadlockOccurred(boolean deadlockOccurred) { this.deadlockOccurred = deadlockOccurred; }

    public int getTotalMeals() { return totalMeals; }
    public void setTotalMeals(int totalMeals) { this.totalMeals = totalMeals; }

    public String getMealsPerPhilosopher() { return mealsPerPhilosopher; }
    public void setMealsPerPhilosopher(String mealsPerPhilosopher) { this.mealsPerPhilosopher = mealsPerPhilosopher; }

    public long getMaxWaitMs() { return maxWaitMs; }
    public void setMaxWaitMs(long maxWaitMs) { this.maxWaitMs = maxWaitMs; }

    public int getFairnessMetric() { return fairnessMetric; }
    public void setFairnessMetric(int fairnessMetric) { this.fairnessMetric = fairnessMetric; }
}
