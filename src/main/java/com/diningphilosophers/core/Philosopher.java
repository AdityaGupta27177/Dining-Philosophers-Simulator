package com.diningphilosophers.core;

import com.diningphilosophers.strategy.DiningStrategy;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Philosopher thread simulating the continuous lifecycle:
 * Thinking -> Hungry -> Fork Acquisition (Critical Section entry) -> Eating -> Release Forks.
 *
 * Syllabus Concepts:
 * 1. THREAD / PROCESS:
 *    Each philosopher represents an independent concurrent process competing for shared resources.
 * 2. CRITICAL SECTION:
 *    The Eating phase requires simultaneous access to two shared resources (left and right forks).
 * 3. STARVATION:
 *    Philosophers track wait times (from becoming HUNGRY until entering EATING) to detect unfairness
 *    or starvation.
 * 4. NO PREEMPTION BREAKING:
 *    The preempt() method forcibly revokes a held fork when resolving deadlocks.
 */
public class Philosopher implements Runnable {
    private final int id;
    private final String name;
    private final Fork leftFork;
    private final Fork rightFork;
    private final EventBus eventBus;
    private final Random random = new Random();

    private volatile DiningStrategy strategy;
    private volatile PhilosopherState state = PhilosopherState.THINKING;
    private final List<Integer> heldForks = new CopyOnWriteArrayList<>();
    private volatile Integer waitingForForkId = null;

    private final AtomicInteger mealsEaten = new AtomicInteger(0);
    private final AtomicLong longestWaitTimeMs = new AtomicLong(0);
    private volatile long currentWaitStartTime = 0;

    private volatile int thinkMs = 500;
    private volatile int eatMs = 500;
    private volatile boolean running = false;
    private volatile boolean preemptRequested = false;
    private volatile Thread runnerThread = null;

    public Philosopher(int id, Fork leftFork, Fork rightFork, DiningStrategy strategy, EventBus eventBus) {
        this.id = id;
        this.name = "Philosopher-" + id;
        this.leftFork = leftFork;
        this.rightFork = rightFork;
        this.strategy = strategy;
        this.eventBus = eventBus;
    }

    @Override
    public void run() {
        this.runnerThread = Thread.currentThread();
        this.running = true;

        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                // 1. THINKING PHASE
                executeThinking();

                if (!running || Thread.currentThread().isInterrupted()) break;

                // 2. HUNGRY PHASE (Requesting resources)
                executeHungry();

                // 3. ACQUIRING FORKS (Entry Protocol to Critical Section)
                boolean acquired = false;
                try {
                    strategy.acquireForks(this, leftFork, rightFork);
                    acquired = true;
                } catch (InterruptedException ie) {
                    if (preemptRequested) {
                        handlePreemptionRecovery();
                        continue;
                    }
                    // Clean interruption during stop
                    break;
                }

                if (!acquired) {
                    continue;
                }

                // 4. EATING PHASE (CRITICAL SECTION)
                executeEating();

                // 5. RELEASE FORKS (Exit Protocol from Critical Section)
                executeRelease();
            }
        } finally {
            // Clean-up on thread termination: ensure no locks remain orphaned
            cleanUpHeldForks();
            this.state = PhilosopherState.THINKING;
            this.running = false;
        }
    }

    private void executeThinking() {
        this.state = PhilosopherState.THINKING;
        this.waitingForForkId = null;
        eventBus.publish(new SimulationEvent(EventType.THINKING, id, name + " is thinking"));
        try {
            // Add small jitter (±20%) for realistic non-deterministic concurrency
            int jitter = random.nextInt(Math.max(1, thinkMs / 5 + 1));
            int sleepTime = Math.max(10, thinkMs + (random.nextBoolean() ? jitter : -jitter));
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void executeHungry() {
        this.state = PhilosopherState.HUNGRY;
        this.currentWaitStartTime = System.currentTimeMillis();
        eventBus.publish(new SimulationEvent(EventType.HUNGRY, id, name + " became hungry"));
    }

    private void executeEating() {
        long waitDuration = System.currentTimeMillis() - currentWaitStartTime;
        longestWaitTimeMs.accumulateAndGet(waitDuration, Math::max);

        this.state = PhilosopherState.EATING;
        this.waitingForForkId = null;
        int mealCount = mealsEaten.incrementAndGet();

        eventBus.publish(new SimulationEvent(
                EventType.EATING,
                id,
                null,
                name + " is eating meal #" + mealCount + " (waited " + waitDuration + "ms)",
                "Wait: " + waitDuration + "ms"
        ));

        try {
            Thread.sleep(Math.max(10, eatMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void executeRelease() {
        try {
            strategy.releaseForks(this, leftFork, rightFork);
        } finally {
            heldForks.clear();
            eventBus.publish(new SimulationEvent(EventType.RELEASED_FORKS, id, name + " finished eating and released both forks"));
        }
    }

    /**
     * Demonstrates breaking NO PREEMPTION condition:
     * When deadlock recovery selects this philosopher as a victim, its held fork is forcibly revoked,
     * allowing adjacent blocked threads to proceed.
     */
    private void handlePreemptionRecovery() {
        preemptRequested = false;
        cleanUpHeldForks();
        this.waitingForForkId = null;
        this.state = PhilosopherState.THINKING;

        eventBus.publish(new SimulationEvent(
                EventType.VICTIM_PREEMPTED,
                id,
                name + " was preempted during deadlock recovery. Released held fork and backed off."
        ));

        try {
            // Back off briefly before re-competing
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized void requestPreemption() {
        this.preemptRequested = true;
        if (runnerThread != null && runnerThread.isAlive()) {
            runnerThread.interrupt();
        }
    }

    public void recordForkAcquired(Fork fork) {
        if (!heldForks.contains(fork.getId())) {
            heldForks.add(fork.getId());
        }
        eventBus.publish(new SimulationEvent(
                EventType.FORK_ACQUIRED,
                id,
                fork.getId(),
                name + " picked up Fork " + fork.getId(),
                "Held forks: " + heldForks
        ));
    }

    public void recordForkReleased(Fork fork) {
        heldForks.remove(Integer.valueOf(fork.getId()));
    }

    public void recordWaitingForFork(Fork fork) {
        this.state = PhilosopherState.BLOCKED;
        this.waitingForForkId = fork.getId();
        eventBus.publish(new SimulationEvent(
                EventType.BLOCKED,
                id,
                fork.getId(),
                name + " is BLOCKED waiting for Fork " + fork.getId() + " (held by " + fork.getCurrentOwnerId() + ")",
                "Wait-for target: Fork-" + fork.getId()
        ));
    }

    public void clearWaiting() {
        this.waitingForForkId = null;
    }

    private void cleanUpHeldForks() {
        try {
            if (heldForks.contains(leftFork.getId())) {
                leftFork.release(id);
            }
        } catch (Exception ignored) {}

        try {
            if (heldForks.contains(rightFork.getId())) {
                rightFork.release(id);
            }
        } catch (Exception ignored) {}

        heldForks.clear();
        waitingForForkId = null;
    }

    public void stop() {
        this.running = false;
        if (runnerThread != null && runnerThread.isAlive()) {
            runnerThread.interrupt();
        }
    }

    public void reset() {
        stop();
        cleanUpHeldForks();
        this.mealsEaten.set(0);
        this.longestWaitTimeMs.set(0);
        this.currentWaitStartTime = 0;
        this.state = PhilosopherState.THINKING;
        this.preemptRequested = false;
    }

    // Getters and Setters
    public int getId() { return id; }
    public String getName() { return name; }
    public Fork getLeftFork() { return leftFork; }
    public Fork getRightFork() { return rightFork; }
    public PhilosopherState getState() { return state; }
    public List<Integer> getHeldForks() { return Collections.unmodifiableList(heldForks); }
    public Integer getWaitingForForkId() { return waitingForForkId; }
    public int getMealsEaten() { return mealsEaten.get(); }
    public long getLongestWaitTimeMs() { return longestWaitTimeMs.get(); }
    public Thread getRunnerThread() { return runnerThread; }
    public void setStrategy(DiningStrategy strategy) { this.strategy = strategy; }
    public void setThinkMs(int thinkMs) { this.thinkMs = thinkMs; }
    public void setEatMs(int eatMs) { this.eatMs = eatMs; }
    public boolean isRunning() { return running; }
}
