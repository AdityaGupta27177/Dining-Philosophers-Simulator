package com.diningphilosophers.web;

public class ForkDto {
    private int id;
    private boolean held;
    private Integer currentOwnerId;

    public ForkDto() {}

    public ForkDto(int id, boolean held, Integer currentOwnerId) {
        this.id = id;
        this.held = held;
        this.currentOwnerId = currentOwnerId;
    }

    public int getId() { return id; }
    public boolean isHeld() { return held; }
    public Integer getCurrentOwnerId() { return currentOwnerId; }
}
