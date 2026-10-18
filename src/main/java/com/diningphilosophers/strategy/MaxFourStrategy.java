package com.diningphilosophers.strategy;

import com.diningphilosophers.core.Fork;
import com.diningphilosophers.core.Philosopher;

import java.util.concurrent.Semaphore;

/**
 * Max-N-Minus-One Strategy (e.g., Semaphore(4) for 5 philosophers):
 * Uses a counting Semaphore to restrict the number of philosophers simultaneously attempting to dine
 * to at most N - 1 (e.g., at most 4 philosophers at a table of 5).
 *
 * Syllabus Concepts & Coffman Conditions:
 * - BREAKS: HOLD AND WAIT / RESOURCE SATURATION
 *   By the Pigeonhole Principle, if at most (N - 1) philosophers compete for N forks,
 *   at least one philosopher is guaranteed to find both of their required forks available,
 *   eat, and release them.
 * - PREVENTS: CIRCULAR WAIT, because the cycle cannot close with an empty chair.
 */
public class MaxFourStrategy implements DiningStrategy {
    private volatile Semaphore tableFootman;

    public MaxFourStrategy() {
        this(5); // Default for 5 philosophers: permit limit = 4
    }

    public MaxFourStrategy(int philosopherCount) {
        reset(philosopherCount);
    }

    @Override
    public synchronized void reset(int philosopherCount) {
        int permits = Math.max(1, philosopherCount - 1);
        this.tableFootman = new Semaphore(permits, true); // Fair semaphore
    }

    @Override
    public void acquireForks(Philosopher philosopher, Fork leftFork, Fork rightFork) throws InterruptedException {
        // Step 1: Acquire permit to sit at the table and attempt dining
        tableFootman.acquire();

        try {
            // Step 2: Pick up left fork
            leftFork.acquire(philosopher.getId());
            philosopher.recordForkAcquired(leftFork);

            // Step 3: Pick up right fork
            philosopher.recordWaitingForFork(rightFork);
            rightFork.acquire(philosopher.getId());
            philosopher.clearWaiting();
            philosopher.recordForkAcquired(rightFork);
        } catch (InterruptedException ie) {
            // In case of interruption, release the table permit so others can proceed
            tableFootman.release();
            throw ie;
        }
    }

    @Override
    public void releaseForks(Philosopher philosopher, Fork leftFork, Fork rightFork) {
        try {
            rightFork.release(philosopher.getId());
            philosopher.recordForkReleased(rightFork);
        } catch (Exception ignored) {}

        try {
            leftFork.release(philosopher.getId());
            philosopher.recordForkReleased(leftFork);
        } catch (Exception ignored) {}

        // Release the seat permit back to the table
        tableFootman.release();
    }

    @Override
    public String getName() {
        return "Max-Four (Semaphore Limiter)";
    }

    @Override
    public String getDescription() {
        return "Limits table occupancy to (N - 1) using a counting Semaphore. Ensures at least one philosopher can acquire both forks.";
    }

    @Override
    public String getBrokenDeadlockCondition() {
        return "Hold and Wait (Pigeonhole guarantee)";
    }
}
