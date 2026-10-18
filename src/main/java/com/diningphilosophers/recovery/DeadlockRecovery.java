package com.diningphilosophers.recovery;

import com.diningphilosophers.core.*;
import com.diningphilosophers.detection.DeadlockDetector;
import com.diningphilosophers.detection.WaitForGraph;

import java.util.Comparator;
import java.util.List;

/**
 * Deadlock Recovery Module.
 *
 * Syllabus Concepts:
 * - BREAKING NO PREEMPTION:
 *   When a deadlock occurs, one of the Coffman conditions must be broken to resolve it.
 *   This recovery mechanism selects a "victim" thread involved in the deadlock cycle and
 *   forcibly revokes its held resource (preemption), rolling the thread back to THINKING.
 *   This frees the contested resource for the neighboring thread, breaking the circular wait.
 */
public class DeadlockRecovery {

    /**
     * Executes preemption recovery by selecting a victim philosopher from the deadlock cycle
     * and forcibly revoking its held fork.
     */
    public static boolean recover(Table table, DeadlockDetector detector, EventBus eventBus) {
        List<Integer> cycle = detector.getLastCycleNodes();

        // If cycle was not cached, reconstruct from current WFG
        if (cycle.isEmpty()) {
            WaitForGraph wfg = WaitForGraph.build(table);
            cycle = wfg.findCycle();
        }

        Philosopher victim = null;

        if (!cycle.isEmpty()) {
            // Select victim from the cycle (e.g., the one with the fewest meals to ensure fairness)
            victim = cycle.stream()
                    .filter(id -> id < table.getPhilosopherCount())
                    .map(table::getPhilosopher)
                    .min(Comparator.comparingInt(Philosopher::getMealsEaten))
                    .orElse(table.getPhilosopher(cycle.get(0)));
        } else {
            // Fallback: Find any blocked philosopher currently holding a fork
            victim = table.getPhilosophers().stream()
                    .filter(p -> p.getState() == PhilosopherState.BLOCKED && !p.getHeldForks().isEmpty())
                    .findFirst()
                    .orElse(null);
        }

        if (victim == null) {
            return false;
        }

        // Forcibly preempt the victim's held resource (breaks No Preemption condition)
        victim.requestPreemption();

        eventBus.publish(new SimulationEvent(
                EventType.DEADLOCK_RECOVERED,
                victim.getId(),
                null,
                "RECOVERY ACTION EXECUTED: Preempted held fork from Philosopher " + victim.getId() + " to break Circular Wait.",
                "Victim: Philosopher-" + victim.getId() + ", Meals: " + victim.getMealsEaten()
        ));

        // Recheck deadlock immediately to update status
        detector.checkDeadlock();
        return true;
    }
}
