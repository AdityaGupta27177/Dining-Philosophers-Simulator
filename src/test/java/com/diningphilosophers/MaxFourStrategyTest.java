package com.diningphilosophers;

import com.diningphilosophers.core.SimulationEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test verifying that MaxFourStrategy NEVER deadlocks.
 *
 * Syllabus Concepts:
 * - BREAKS: HOLD AND WAIT / RESOURCE SATURATION
 *   By restricting simultaneous diners to at most N - 1 using a Semaphore, the Pigeonhole Principle
 *   guarantees that at least one philosopher can acquire both forks and eat, completely preventing deadlocks.
 */
public class MaxFourStrategyTest {

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
    @Timeout(value = 8, unit = TimeUnit.SECONDS)
    public void testMaxFourStrategyNeverDeadlocks() throws InterruptedException {
        // High contention: 5 philosophers with short think/eat times
        engine.start("maxfour", 5, 20, 20);

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 3000) {
            assertFalse(engine.getDetector().isDeadlocked(), "Deadlock occurred unexpectedly in MaxFourStrategy!");
            Thread.sleep(100);
        }

        // Verify that philosophers made progress and ate meals
        int totalMeals = engine.getMetrics().getTotalMeals(engine.getTable().getPhilosophers());
        assertTrue(totalMeals > 0, "Expected philosophers to make progress and consume meals, got: " + totalMeals);
        assertFalse(engine.getDetector().isDeadlocked(), "Deadlock detected at test conclusion.");
    }
}
