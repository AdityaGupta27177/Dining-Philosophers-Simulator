package com.diningphilosophers.web;

import java.util.List;

public class SimulationStateDto {
    private boolean running;
    private boolean deadlocked;
    private String cyclePath;
    private List<Integer> cycleNodes;
    private String strategyName;
    private String strategyDescription;
    private String brokenCondition;
    private int philosopherCount;
    private int thinkMs;
    private int eatMs;
    private double durationSec;
    private int totalMeals;
    private long maxWaitMs;
    private int fairness;
    private List<PhilosopherDto> philosophers;
    private List<ForkDto> forks;

    public SimulationStateDto() {}

    public SimulationStateDto(boolean running, boolean deadlocked, String cyclePath,
                              List<Integer> cycleNodes, String strategyName,
                              String strategyDescription, String brokenCondition,
                              int philosopherCount, int thinkMs, int eatMs,
                              double durationSec, int totalMeals, long maxWaitMs,
                              int fairness, List<PhilosopherDto> philosophers, List<ForkDto> forks) {
        this.running = running;
        this.deadlocked = deadlocked;
        this.cyclePath = cyclePath;
        this.cycleNodes = cycleNodes;
        this.strategyName = strategyName;
        this.strategyDescription = strategyDescription;
        this.brokenCondition = brokenCondition;
        this.philosopherCount = philosopherCount;
        this.thinkMs = thinkMs;
        this.eatMs = eatMs;
        this.durationSec = durationSec;
        this.totalMeals = totalMeals;
        this.maxWaitMs = maxWaitMs;
        this.fairness = fairness;
        this.philosophers = philosophers;
        this.forks = forks;
    }

    public boolean isRunning() { return running; }
    public boolean isDeadlocked() { return deadlocked; }
    public String getCyclePath() { return cyclePath; }
    public List<Integer> getCycleNodes() { return cycleNodes; }
    public String getStrategyName() { return strategyName; }
    public String getStrategyDescription() { return strategyDescription; }
    public String getBrokenCondition() { return brokenCondition; }
    public int getPhilosopherCount() { return philosopherCount; }
    public int getThinkMs() { return thinkMs; }
    public int getEatMs() { return eatMs; }
    public double getDurationSec() { return durationSec; }
    public int getTotalMeals() { return totalMeals; }
    public long getMaxWaitMs() { return maxWaitMs; }
    public int getFairness() { return fairness; }
    public List<PhilosopherDto> getPhilosophers() { return philosophers; }
    public List<ForkDto> getForks() { return forks; }
}
