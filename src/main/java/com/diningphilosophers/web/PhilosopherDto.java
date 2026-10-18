package com.diningphilosophers.web;

import java.util.List;

public class PhilosopherDto {
    private int id;
    private String name;
    private String state;
    private List<Integer> heldForks;
    private Integer waitingForForkId;
    private int mealsEaten;
    private long longestWaitTimeMs;
    private int leftForkId;
    private int rightForkId;

    public PhilosopherDto() {}

    public PhilosopherDto(int id, String name, String state, List<Integer> heldForks,
                          Integer waitingForForkId, int mealsEaten, long longestWaitTimeMs,
                          int leftForkId, int rightForkId) {
        this.id = id;
        this.name = name;
        this.state = state;
        this.heldForks = heldForks;
        this.waitingForForkId = waitingForForkId;
        this.mealsEaten = mealsEaten;
        this.longestWaitTimeMs = longestWaitTimeMs;
        this.leftForkId = leftForkId;
        this.rightForkId = rightForkId;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getState() { return state; }
    public List<Integer> getHeldForks() { return heldForks; }
    public Integer getWaitingForForkId() { return waitingForForkId; }
    public int getMealsEaten() { return mealsEaten; }
    public long getLongestWaitTimeMs() { return longestWaitTimeMs; }
    public int getLeftForkId() { return leftForkId; }
    public int getRightForkId() { return rightForkId; }
}
