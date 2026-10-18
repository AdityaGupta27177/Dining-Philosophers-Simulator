package com.diningphilosophers.core;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents a shared Fork resource situated between adjacent philosophers at the table.
 *
 * Syllabus Concepts:
 * 1. MUTUAL EXCLUSION:
 *    A Fork can only be held by at most one Philosopher at any time. This is strictly enforced
 *    by wrapping a Java ReentrantLock with fairness enabled.
 * 2. CRITICAL SECTION:
 *    Acquiring both left and right forks grants the philosopher exclusive access to the
 *    Critical Section (Eating).
 * 3. NO PREEMPTION:
 *    Under standard conditions, a fork cannot be forcibly taken away from a philosopher holding it;
 *    it can only be released voluntarily. The DeadlockRecovery module demonstrates breaking this condition.
 */
public class Fork {
    private final int id;
    private final ReentrantLock lock;
    private volatile Integer currentOwnerId = null;

    public Fork(int id) {
        this.id = id;
        // Fair lock ensures FIFO queueing for threads waiting on this resource
        this.lock = new ReentrantLock(true);
    }

    public int getId() {
        return id;
    }

    /**
     * Attempts to acquire the fork lock interruptibly.
     * Throws InterruptedException if the waiting thread is interrupted,
     * which facilitates clean thread termination and deadlock preemption.
     */
    public void acquire(int philosopherId) throws InterruptedException {
        lock.lockInterruptibly();
        this.currentOwnerId = philosopherId;
    }

    /**
     * Attempts to acquire the fork with a bounded timeout.
     */
    public boolean tryAcquire(int philosopherId, long timeoutMs) throws InterruptedException {
        if (lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS)) {
            this.currentOwnerId = philosopherId;
            return true;
        }
        return false;
    }

    /**
     * Releases the fork if held by the current thread.
     */
    public void release(int philosopherId) {
        if (lock.isHeldByCurrentThread()) {
            this.currentOwnerId = null;
            lock.unlock();
        }
    }

    /**
     * Checks if this fork is currently acquired by any philosopher thread.
     */
    public boolean isHeld() {
        return lock.isLocked();
    }

    /**
     * Returns the philosopher ID currently holding this fork, or null if free.
     */
    public Integer getCurrentOwnerId() {
        return currentOwnerId;
    }

    /**
     * Internal access to ReentrantLock for deadlock detection and state inspection.
     */
    public ReentrantLock getLock() {
        return lock;
    }

    @Override
    public String toString() {
        return "Fork-" + id + " [held=" + isHeld() + ", owner=" + currentOwnerId + "]";
    }
}
