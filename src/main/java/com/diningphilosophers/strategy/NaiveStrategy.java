package com.diningphilosophers.strategy;

import com.diningphilosophers.core.Fork;
import com.diningphilosophers.core.Philosopher;

/**
 * Naive Strategy (Deadlock-prone):
 * Every philosopher picks up their left fork first, then waits and attempts to pick up their right fork.
 *
 * Syllabus Concepts & Coffman Conditions:
 * 1. MUTUAL EXCLUSION: Each fork is wrapped in an exclusive ReentrantLock.
 * 2. HOLD AND WAIT: A philosopher holds their left fork while waiting for the right fork.
 * 3. NO PREEMPTION: Held forks cannot be preempted or revoked.
 * 4. CIRCULAR WAIT: Each philosopher P(i) holds Fork(i) and waits for Fork((i+1)%N) held by P((i+1)%N).
 *
 * Result: All 4 Coffman conditions hold simultaneously. With the artificial pause between picks,
 * every philosopher picks up their left fork concurrently, creating an unavoidable circular deadlock.
 */
public class NaiveStrategy implements DiningStrategy {
    private final int pauseBetweenForksMs;

    public NaiveStrategy() {
        this(100);
    }

    public NaiveStrategy(int pauseBetweenForksMs) {
        this.pauseBetweenForksMs = pauseBetweenForksMs;
    }

    @Override
    public void acquireForks(Philosopher philosopher, Fork leftFork, Fork rightFork) throws InterruptedException {
        // Step 1: Pick up left fork
        leftFork.acquire(philosopher.getId());
        philosopher.recordForkAcquired(leftFork);

        // DELIBERATE PAUSE: Guarantees that all adjacent philosophers pick up their left fork
        // before any can grab their right fork, reliably demonstrating the HOLD AND WAIT condition.
        if (pauseBetweenForksMs > 0) {
            Thread.sleep(pauseBetweenForksMs);
        }

        // Step 2: Attempt to pick up right fork (enters BLOCKED state if held)
        philosopher.recordWaitingForFork(rightFork);
        rightFork.acquire(philosopher.getId());
        philosopher.clearWaiting();
        philosopher.recordForkAcquired(rightFork);
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
    }

    @Override
    public String getName() {
        return "Naive (Deadlock-Prone)";
    }

    @Override
    public String getDescription() {
        return "Picks left fork first, pauses, then right fork. Satisfies all 4 Coffman conditions, reliably triggering a deadlock.";
    }

    @Override
    public String getBrokenDeadlockCondition() {
        return "None (Deadlock Occurs)";
    }
}
