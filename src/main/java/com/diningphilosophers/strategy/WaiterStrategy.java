package com.diningphilosophers.strategy;

import com.diningphilosophers.core.Fork;
import com.diningphilosophers.core.Philosopher;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Waiter Strategy (Central Monitor / All-or-Nothing Allocation):
 * A centralized monitor (the "Head Waiter") coordinates all fork requests.
 * A philosopher requests both forks simultaneously. The waiter grants permission ONLY if
 * BOTH forks are currently free. If either fork is in use, the philosopher waits without holding
 * ANY fork.
 *
 * Syllabus Concepts & Coffman Conditions:
 * - BREAKS: HOLD AND WAIT
 *   In this design, a thread never holds a resource while waiting for another.
 *   Resource allocation is atomic from the perspective of the table: either both forks are acquired,
 *   or none are held. Therefore, hold-and-wait cannot occur, making deadlock impossible.
 */
public class WaiterStrategy implements DiningStrategy {
    private final ReentrantLock waiterLock = new ReentrantLock(true);
    private final Condition forksAvailable = waiterLock.newCondition();

    @Override
    public void acquireForks(Philosopher philosopher, Fork leftFork, Fork rightFork) throws InterruptedException {
        waiterLock.lockInterruptibly();
        try {
            // Wait until BOTH forks are completely free
            while (leftFork.isHeld() || rightFork.isHeld()) {
                philosopher.recordWaitingForFork(leftFork.isHeld() ? leftFork : rightFork);
                forksAvailable.await();
            }

            // Both forks are guaranteed free: acquire both atomically under monitor protection
            leftFork.acquire(philosopher.getId());
            philosopher.recordForkAcquired(leftFork);

            rightFork.acquire(philosopher.getId());
            philosopher.recordForkAcquired(rightFork);

            philosopher.clearWaiting();
        } finally {
            waiterLock.unlock();
        }
    }

    @Override
    public void releaseForks(Philosopher philosopher, Fork leftFork, Fork rightFork) {
        waiterLock.lock();
        try {
            try {
                rightFork.release(philosopher.getId());
                philosopher.recordForkReleased(rightFork);
            } catch (Exception ignored) {}

            try {
                leftFork.release(philosopher.getId());
                philosopher.recordForkReleased(leftFork);
            } catch (Exception ignored) {}

            // Notify all waiting philosophers that forks have become free
            forksAvailable.signalAll();
        } finally {
            waiterLock.unlock();
        }
    }

    @Override
    public String getName() {
        return "Central Waiter (All-or-Nothing Monitor)";
    }

    @Override
    public String getDescription() {
        return "Central monitor grants both forks simultaneously or none. Completely eliminates Hold-and-Wait.";
    }

    @Override
    public String getBrokenDeadlockCondition() {
        return "Hold and Wait (Atomic Acquisition)";
    }
}
