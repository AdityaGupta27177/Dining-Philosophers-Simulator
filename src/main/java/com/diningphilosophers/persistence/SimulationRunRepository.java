package com.diningphilosophers.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for persisting and retrieving simulation history.
 */
@Repository
public interface SimulationRunRepository extends JpaRepository<SimulationRun, Long> {
    List<SimulationRun> findAllByOrderByIdDesc();
}
