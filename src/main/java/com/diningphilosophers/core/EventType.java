package com.diningphilosophers.core;

/**
 * Types of simulation events emitted across the lifecycle of the philosophers and synchronization resources.
 */
public enum EventType {
    SIMULATION_STARTED,
    SIMULATION_STOPPED,
    SIMULATION_RESET,
    THINKING,
    HUNGRY,
    ACQUIRING_FORK,
    FORK_ACQUIRED,
    EATING,
    RELEASED_FORKS,
    BLOCKED,
    DEADLOCK_DETECTED,
    DEADLOCK_RECOVERED,
    VICTIM_PREEMPTED
}
