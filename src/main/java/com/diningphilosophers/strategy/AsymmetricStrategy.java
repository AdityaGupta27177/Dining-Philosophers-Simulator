package com.diningphilosophers.strategy;

import com.diningphilosophers.core.Fork;
import com.diningphilosophers.core.Philosopher;

/**
 * Asymmetric Strategy (Dijkstra's Symmetry-Breaking Solution):
 * Odd-numbered philosophers pick up their right fork first, then left fork.
 * Even-numbered philosophers pick up their left fork first, then right fork.
 *
 * Syllabus Concepts & Coffman Conditions:
 * - BREAKS: CIRCULAR WAIT
 *   Symmetric systems allow every philosopher to concurrently hold their left fork and wait on the right.
 *   By introducing asymmetry, neighboring philosophers (e.g. P0 and P1) both reach for the shared fork
 *   (Fork 1) as their first resource. One wins and the other immediately blocks without holding any fork.
 *   This prevents the simultaneous circular hold-and-wait dependency chain.
 */
public class AsymmetricStrategy implements DiningStrategy {

    @Override
    public void acquireForks(Philosopher philosopher, Fork leftFork, Fork rightFork) throws InterruptedException {
        boolean isOdd = (philosopher.getId() % 2 != 0);
        Fork firstFork = isOdd ? rightFork : leftFork;
        Fork secondFork = isOdd ? leftFork : rightFork;

        // Step 1: Pick up first fork according to parity
        firstFork.acquire(philosopher.getId());
        philosopher.recordForkAcquired(firstFork);

        // Step 2: Pick up second fork
        philosopher.recordWaitingForFork(secondFork);
        secondFork.acquire(philosopher.getId());
        philosopher.clearWaiting();
        philosopher.recordForkAcquired(secondFork);
    }

    @Override
    public void releaseForks(Philosopher philosopher, Fork leftFork, Fork rightFork) {
        boolean isOdd = (philosopher.getId() % 2 != 0);
        Fork firstFork = isOdd ? rightFork : leftFork;
        Fork secondFork = isOdd ? leftFork : rightFork;

        try {
            secondFork.release(philosopher.getId());
            philosopher.recordForkReleased(secondFork);
        } catch (Exception ignored) {}

        try {
            firstFork.release(philosopher.getId());
            philosopher.recordForkReleased(firstFork);
        } catch (Exception ignored) {}
    }

    @Override
    public String getName() {
        return "Asymmetric (Odd-Right, Even-Left)";
    }

    @Override
    public String getDescription() {
        return "Odd philosophers pick right fork first; even philosophers pick left fork first. Breaks circular wait by breaking symmetry.";
    }

    @Override
    public String getBrokenDeadlockCondition() {
        return "Circular Wait (Symmetry Breaking)";
    }
}
