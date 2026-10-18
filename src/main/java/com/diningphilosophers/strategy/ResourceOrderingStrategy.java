package com.diningphilosophers.strategy;

import com.diningphilosophers.core.Fork;
import com.diningphilosophers.core.Philosopher;

/**
 * Resource Ordering Strategy (Havender's Linear Ordering Principle):
 * Imposes a global strict total order on all shared resources (Forks 0 to N-1).
 * Every philosopher must always request and lock the lower-numbered fork first,
 * followed by the higher-numbered fork.
 *
 * Syllabus Concepts & Coffman Conditions:
 * - BREAKS: CIRCULAR WAIT
 *   A circular wait requires a cycle of threads waiting for resources held by others:
 *   P0 -> P1 -> P2 -> ... -> P0.
 *   Under strict resource ordering, every thread only requests higher-numbered resources than
 *   those it currently holds (or acquires lowest first). For example, Philosopher N-1 has left fork (N-1)
 *   and right fork 0. Instead of requesting N-1 then 0, it requests Fork 0 first!
 *   Therefore, no cycle can ever form in the Resource Allocation Graph.
 */
public class ResourceOrderingStrategy implements DiningStrategy {

    @Override
    public void acquireForks(Philosopher philosopher, Fork leftFork, Fork rightFork) throws InterruptedException {
        // Enforce strict global resource hierarchy: lower ID fork is acquired first
        Fork firstFork = (leftFork.getId() < rightFork.getId()) ? leftFork : rightFork;
        Fork secondFork = (leftFork.getId() < rightFork.getId()) ? rightFork : leftFork;

        // Step 1: Acquire lower-numbered fork
        firstFork.acquire(philosopher.getId());
        philosopher.recordForkAcquired(firstFork);

        // Step 2: Acquire higher-numbered fork
        philosopher.recordWaitingForFork(secondFork);
        secondFork.acquire(philosopher.getId());
        philosopher.clearWaiting();
        philosopher.recordForkAcquired(secondFork);
    }

    @Override
    public void releaseForks(Philosopher philosopher, Fork leftFork, Fork rightFork) {
        Fork firstFork = (leftFork.getId() < rightFork.getId()) ? leftFork : rightFork;
        Fork secondFork = (leftFork.getId() < rightFork.getId()) ? rightFork : leftFork;

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
        return "Resource Ordering (Global Hierarchy)";
    }

    @Override
    public String getDescription() {
        return "Always acquires the lower-numbered fork before the higher-numbered fork. Breaks Circular Wait by imposing a strict total order on resources.";
    }

    @Override
    public String getBrokenDeadlockCondition() {
        return "Circular Wait (Strict Total Ordering)";
    }
}
