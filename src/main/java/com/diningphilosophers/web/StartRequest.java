package com.diningphilosophers.web;

/**
 * Payload for starting a simulation run.
 */
public class StartRequest {
    private String strategy = "naive";
    private int philosophers = 5;
    private int thinkMs = 500;
    private int eatMs = 500;

    public StartRequest() {}

    public StartRequest(String strategy, int philosophers, int thinkMs, int eatMs) {
        this.strategy = strategy;
        this.philosophers = philosophers;
        this.thinkMs = thinkMs;
        this.eatMs = eatMs;
    }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public int getPhilosophers() { return philosophers; }
    public void setPhilosophers(int philosophers) { this.philosophers = philosophers; }

    public int getThinkMs() { return thinkMs; }
    public void setThinkMs(int thinkMs) { this.thinkMs = thinkMs; }

    public int getEatMs() { return eatMs; }
    public void setEatMs(int eatMs) { this.eatMs = eatMs; }
}
