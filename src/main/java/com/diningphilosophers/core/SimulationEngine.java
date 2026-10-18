package com.diningphilosophers.core;

import com.diningphilosophers.detection.DeadlockDetector;
import com.diningphilosophers.recovery.DeadlockRecovery;
import com.diningphilosophers.strategy.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Core simulation orchestrator managing Thread pools, Table lifecycle, and Deadlock Detection.
 * Pure Java (no Spring annotations in core).
 */
public class SimulationEngine {
    private final EventBus eventBus;
    private Table table;
    private DiningStrategy strategy;
    private DeadlockDetector detector;
    private Metrics metrics;
    private ExecutorService executorService;

    private int philosopherCount = 5;
    private int thinkMs = 500;
    private int eatMs = 500;
    private volatile boolean running = false;

    private Consumer<Metrics> onSimulationCompletedListener;

    public SimulationEngine() {
        this.eventBus = new EventBus();
        this.strategy = new NaiveStrategy();
        this.metrics = new Metrics();
        initTable();
    }

    private void initTable() {
        this.table = new Table(philosopherCount, strategy, eventBus, thinkMs, eatMs);
        this.detector = new DeadlockDetector(table, eventBus, cyclePath -> {
            metrics.markDeadlock();
            notifyCompletion();
        });
    }

    public synchronized void start(String strategyKey, int count, int thinkTime, int eatTime) {
        if (running) {
            stop();
        }

        this.philosopherCount = Math.max(3, Math.min(8, count));
        this.thinkMs = Math.max(10, thinkTime);
        this.eatMs = Math.max(10, eatTime);
        this.strategy = resolveStrategy(strategyKey, this.philosopherCount);
        this.metrics = new Metrics();

        initTable();

        this.running = true;
        this.executorService = Executors.newFixedThreadPool(philosopherCount);

        // Start philosopher worker threads
        for (Philosopher p : table.getPhilosophers()) {
            executorService.submit(p);
        }

        // Start background deadlock detector
        detector.start();

        eventBus.publish(new SimulationEvent(
                EventType.SIMULATION_STARTED,
                "Simulation started with " + philosopherCount + " philosophers using strategy: " + strategy.getName()
        ));
    }

    public synchronized void stop() {
        if (!running) return;
        this.running = false;

        // Stop deadlock detector
        if (detector != null) {
            detector.stop();
        }

        // Signal philosophers to stop
        if (table != null) {
            for (Philosopher p : table.getPhilosophers()) {
                p.stop();
            }
        }

        // Cleanly terminate executor pool
        if (executorService != null) {
            executorService.shutdownNow();
            try {
                if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            executorService = null;
        }

        metrics.markStopped();
        eventBus.publish(new SimulationEvent(EventType.SIMULATION_STOPPED, "Simulation stopped cleanly."));
        notifyCompletion();
    }

    public synchronized void reset() {
        stop();
        if (table != null) {
            table.reset();
        }
        eventBus.clearHistory();
        this.metrics = new Metrics();
        eventBus.publish(new SimulationEvent(EventType.SIMULATION_RESET, "Simulation reset. All metrics and forks cleared."));
    }

    public synchronized boolean recoverDeadlock() {
        if (table != null && detector != null) {
            return DeadlockRecovery.recover(table, detector, eventBus);
        }
        return false;
    }

    private void notifyCompletion() {
        if (onSimulationCompletedListener != null) {
            try {
                onSimulationCompletedListener.accept(metrics);
            } catch (Exception ignored) {}
        }
    }

    public static DiningStrategy resolveStrategy(String key, int count) {
        if (key == null) return new NaiveStrategy();
        String normalized = key.trim().toLowerCase().replaceAll("[^a-z]", "");
        if (normalized.contains("maxfour") || normalized.contains("semaphore") || normalized.contains("limit")) {
            return new MaxFourStrategy(count);
        } else if (normalized.contains("ordering") || normalized.contains("resource") || normalized.contains("hierarchy")) {
            return new ResourceOrderingStrategy();
        } else if (normalized.contains("asymmetric") || normalized.contains("odd") || normalized.contains("even")) {
            return new AsymmetricStrategy();
        } else if (normalized.contains("waiter") || normalized.contains("monitor") || normalized.contains("central")) {
            return new WaiterStrategy();
        } else {
            return new NaiveStrategy();
        }
    }

    public void setOnSimulationCompletedListener(Consumer<Metrics> listener) {
        this.onSimulationCompletedListener = listener;
    }

    public Table getTable() { return table; }
    public DiningStrategy getStrategy() { return strategy; }
    public EventBus getEventBus() { return eventBus; }
    public DeadlockDetector getDetector() { return detector; }
    public Metrics getMetrics() { return metrics; }
    public boolean isRunning() { return running; }
    public int getPhilosopherCount() { return philosopherCount; }
    public int getThinkMs() { return thinkMs; }
    public int getEatMs() { return eatMs; }
}
