package com.diningphilosophers.core;

/**
 * Represents the current execution state of a Philosopher thread in the simulation.
 *
 * Syllabus Concepts:
 * - THINKING: Thread is in an independent computation phase outside the Critical Section.
 * - HUNGRY: Thread is requesting access to shared resources (Forks) before entering the Critical Section.
 * - BLOCKED: Thread is waiting for a contested resource, which may contribute to Hold-and-Wait and Circular Wait.
 * - EATING: Thread has successfully acquired all required resources and entered the Critical Section.
 */
public enum PhilosopherState {
    THINKING,
    HUNGRY,
    EATING,
    BLOCKED
}
