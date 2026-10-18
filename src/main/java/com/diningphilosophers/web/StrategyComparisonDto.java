package com.diningphilosophers.web;

public class StrategyComparisonDto {
    private String strategyKey;
    private String strategyName;
    private String brokenCondition;
    private boolean deadlockOccurred;
    private int totalMeals;
    private long maxWaitMs;
    private int fairnessMetric;
    private double durationSec;

    public StrategyComparisonDto() {}

    public StrategyComparisonDto(String strategyKey, String strategyName, String brokenCondition,
                                 boolean deadlockOccurred, int totalMeals, long maxWaitMs,
                                 int fairnessMetric, double durationSec) {
        this.strategyKey = strategyKey;
        this.strategyName = strategyName;
        this.brokenCondition = brokenCondition;
        this.deadlockOccurred = deadlockOccurred;
        this.totalMeals = totalMeals;
        this.maxWaitMs = maxWaitMs;
        this.fairnessMetric = fairnessMetric;
        this.durationSec = durationSec;
    }

    public String getStrategyKey() { return strategyKey; }
    public String getStrategyName() { return strategyName; }
    public String getBrokenCondition() { return brokenCondition; }
    public boolean isDeadlockOccurred() { return deadlockOccurred; }
    public int getTotalMeals() { return totalMeals; }
    public long getMaxWaitMs() { return maxWaitMs; }
    public int getFairnessMetric() { return fairnessMetric; }
    public double getDurationSec() { return durationSec; }
}
