package com.diningphilosophers;

import com.diningphilosophers.core.SimulationEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test verifying that NaiveStrategy reliably deadlocks.
 *
 * Syllabus Concepts:
 * In NaiveStrategy, all 4 Coffman conditions hold:
 * 1. Mutual Exclusion (Forks are exclusive locks)
 * 2. Hold and Wait (Philosophers hold left fork and wait for right)
 * 3. No Preemption (Forks cannot be forcibly taken)
 * 4. Circular Wait (P0 -> P1 -> P2 -> P3 -> P4 -> P0)
 *
 * Therefore, under concurrent execution, a deadlock is guaranteed to occur.
 */
public class NaiveStrategyTest {

    private SimulationEngine engine;

    @BeforeEach
    public void setUp() {
        engine = new SimulationEngine();
    }

    @AfterEach
    public void tearDown() {
        if (engine != null) {
            engine.stop();
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testNaiveStrategyDeadlocksReliably() throws InterruptedException {
        // Fast think and eat times to quickly induce deadlock contention
        engine.start("naive", 5, 30, 30);

        long start = System.currentTimeMillis();
        boolean deadlocked = false;

        // Poll for deadlock detection
        while (System.currentTimeMillis() - start < 8000) {
            if (engine.getDetector().isDeadlocked()) {
                deadlocked = true;
                break;
            }
            Thread.sleep(100);
        }

        assertTrue(deadlocked, "Expected NaiveStrategy to produce a circular wait deadlock within timeout.");
        assertTrue(engine.getDetector().getLastCyclePath().contains("->"),
                "Expected cycle path in format P0 -> P1 ... but got: " + engine.getDetector().getLastCyclePath());
    }
}
