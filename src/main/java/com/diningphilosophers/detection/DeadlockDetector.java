package com.diningphilosophers.detection;

import com.diningphilosophers.core.EventBus;
import com.diningphilosophers.core.EventType;
import com.diningphilosophers.core.SimulationEvent;
import com.diningphilosophers.core.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Background Deadlock Detector thread.
 *
 * Syllabus Concepts:
 * - DEADLOCK DETECTION:
 *   Periodically samples the Wait-For Graph (WFG) and runs cycle-detection algorithms (DFS).
 *   When a cycle is detected, publishes a DEADLOCK event with the complete cycle path
 *   (e.g., P0 -> P1 -> P2 -> P3 -> P4 -> P0).
 */
public class DeadlockDetector implements Runnable {
    private final Table table;
    private final EventBus eventBus;
    private final Consumer<String> onDeadlockCallback;
    private final int pollIntervalMs;

    private volatile boolean running = false;
    private volatile boolean deadlocked = false;
    private volatile String lastCyclePath = "";
    private final List<Integer> lastCycleNodes = new CopyOnWriteArrayList<>();
    private Thread detectorThread = null;

    public DeadlockDetector(Table table, EventBus eventBus, Consumer<String> onDeadlockCallback) {
        this(table, eventBus, onDeadlockCallback, 150);
    }

    public DeadlockDetector(Table table, EventBus eventBus, Consumer<String> onDeadlockCallback, int pollIntervalMs) {
        this.table = table;
        this.eventBus = eventBus;
        this.onDeadlockCallback = onDeadlockCallback;
        this.pollIntervalMs = pollIntervalMs;
    }

    public synchronized void start() {
        if (running) return;
        this.running = true;
        this.deadlocked = false;
        this.lastCyclePath = "";
        this.lastCycleNodes.clear();

        this.detectorThread = new Thread(this, "DeadlockDetector-Thread");
        this.detectorThread.setDaemon(true);
        this.detectorThread.start();
    }

    public synchronized void stop() {
        this.running = false;
        if (detectorThread != null && detectorThread.isAlive()) {
            detectorThread.interrupt();
        }
    }

    @Override
    public void run() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(pollIntervalMs);
                checkDeadlock();
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * Inspects the Table by constructing a Wait-For Graph and searching for cycles.
     */
    public void checkDeadlock() {
        WaitForGraph wfg = WaitForGraph.build(table);
        List<Integer> cycle = wfg.findCycle();

        if (!cycle.isEmpty()) {
            String path = WaitForGraph.formatCyclePath(cycle);
            if (!deadlocked) {
                this.deadlocked = true;
                this.lastCyclePath = path;
                this.lastCycleNodes.clear();
                this.lastCycleNodes.addAll(cycle);

                // Publish Deadlock Event with full cycle chain
                eventBus.publish(new SimulationEvent(
                        EventType.DEADLOCK_DETECTED,
                        null,
                        null,
                        "DEADLOCK DETECTED! Cycle: " + path,
                        path
                ));

                if (onDeadlockCallback != null) {
                    onDeadlockCallback.accept(path);
                }
            }
        } else {
            // Cycle resolved (e.g., via recovery preemption)
            if (deadlocked) {
                this.deadlocked = false;
                this.lastCyclePath = "";
                this.lastCycleNodes.clear();
            }
        }
    }

    public boolean isDeadlocked() {
        return deadlocked;
    }

    public String getLastCyclePath() {
        return lastCyclePath;
    }

    public List<Integer> getLastCycleNodes() {
        return Collections.unmodifiableList(new ArrayList<>(lastCycleNodes));
    }
}
