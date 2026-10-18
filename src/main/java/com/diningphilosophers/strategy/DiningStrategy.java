package com.diningphilosophers.strategy;

import com.diningphilosophers.core.Fork;
import com.diningphilosophers.core.Philosopher;

/**
 * Strategy interface defining how a philosopher requests, acquires, and releases forks.
 *
 * Syllabus Connection:
 * Each strategy addresses the four Coffman conditions required for deadlock:
 * 1. Mutual Exclusion
 * 2. Hold and Wait
 * 3. No Preemption
 * 4. Circular Wait
 */
public interface DiningStrategy {
    /**
     * Entry into Critical Section: protocol for acquiring both left and right forks.
     */
    void acquireForks(Philosopher philosopher, Fork leftFork, Fork rightFork) throws InterruptedException;

    /**
     * Exit from Critical Section: protocol for releasing both forks.
     */
    void releaseForks(Philosopher philosopher, Fork leftFork, Fork rightFork);

    /**
     * Unique identifier for this strategy.
     */
    String getName();

    /**
     * Human-readable explanation of how the strategy operates.
     */
    String getDescription();

    /**
     * The Coffman condition broken by this strategy (or "None" if deadlock is possible).
     */
    String getBrokenDeadlockCondition();

    /**
     * Optional hook to reinitialize internal state (e.g., semaphores or monitors) when table resets.
     */
    default void reset(int philosopherCount) {}
}
