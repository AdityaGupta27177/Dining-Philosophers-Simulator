package com.diningphilosophers.web;

import com.diningphilosophers.core.*;
import com.diningphilosophers.persistence.SimulationRun;
import com.diningphilosophers.persistence.SimulationRunRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * REST Controller exposing simulation operations, SSE event streams, and database analytics.
 */
@RestController
@RequestMapping("/api")
public class SimulationController {

    private final SimulationRunRepository runRepository;
    private final ObjectMapper objectMapper;
    private final SimulationEngine engine;
    private final List<SseEmitter> activeEmitters = new CopyOnWriteArrayList<>();
    private final AtomicBoolean runSaved = new AtomicBoolean(false);

    public SimulationController(SimulationRunRepository runRepository, ObjectMapper objectMapper) {
        this.runRepository = runRepository;
        this.objectMapper = objectMapper;
        this.engine = new SimulationEngine();

        // Wire persistence on simulation completion (deadlock or stop)
        this.engine.setOnSimulationCompletedListener(this::saveSimulationRun);

        // Wire EventBus to active SSE emitters
        this.engine.getEventBus().subscribe(this::broadcastEventToEmitters);
    }

    @PostMapping("/start")
    public ResponseEntity<SimulationStateDto> startSimulation(@RequestBody(required = false) StartRequest req) {
        if (req == null) req = new StartRequest();
        runSaved.set(false);
        engine.start(req.getStrategy(), req.getPhilosophers(), req.getThinkMs(), req.getEatMs());
        return ResponseEntity.ok(buildCurrentState());
    }

    @PostMapping("/stop")
    public ResponseEntity<SimulationStateDto> stopSimulation() {
        engine.stop();
        return ResponseEntity.ok(buildCurrentState());
    }

    @PostMapping("/reset")
    public ResponseEntity<SimulationStateDto> resetSimulation() {
        engine.reset();
        runSaved.set(false);
        return ResponseEntity.ok(buildCurrentState());
    }

    @PostMapping("/recover")
    public ResponseEntity<Map<String, Object>> recoverDeadlock() {
        boolean recovered = engine.recoverDeadlock();
        Map<String, Object> resp = new HashMap<>();
        resp.put("recovered", recovered);
        resp.put("message", recovered ?
                "Deadlock successfully resolved! Forcibly preempted resource from victim thread (broken No-Preemption condition)." :
                "No active deadlock cycle detected to recover.");
        resp.put("state", buildCurrentState());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/state")
    public ResponseEntity<SimulationStateDto> getState() {
        return ResponseEntity.ok(buildCurrentState());
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        // 30-minute timeout for SSE stream
        SseEmitter emitter = new SseEmitter(1800000L);
        activeEmitters.add(emitter);

        emitter.onCompletion(() -> activeEmitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            activeEmitters.remove(emitter);
        });
        emitter.onError(e -> activeEmitters.remove(emitter));

        // Send initial connection packet with current state and recent events
        try {
            Map<String, Object> initPayload = new HashMap<>();
            initPayload.put("state", buildCurrentState());
            initPayload.put("recentEvents", engine.getEventBus().getRecentEvents());

            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data(objectMapper.writeValueAsString(initPayload)));
        } catch (IOException e) {
            emitter.complete();
            activeEmitters.remove(emitter);
        }

        return emitter;
    }

    @GetMapping("/history")
    public ResponseEntity<List<SimulationRun>> getHistory() {
        return ResponseEntity.ok(runRepository.findAllByOrderByIdDesc());
    }

    @GetMapping("/compare")
    public ResponseEntity<List<StrategyComparisonDto>> compareStrategies(
            @RequestParam(defaultValue = "15") int durationSec) {
        List<StrategyComparisonDto> results = runComparisonBenchmark(durationSec);
        return ResponseEntity.ok(results);
    }

    private synchronized void saveSimulationRun(Metrics metrics) {
        if (runSaved.compareAndSet(false, true)) {
            try {
                Table table = engine.getTable();
                List<Philosopher> philosophers = (table != null) ? table.getPhilosophers() : Collections.emptyList();

                String mealsJson = objectMapper.writeValueAsString(metrics.getMealsPerPhilosopher(philosophers));
                LocalDateTime startDateTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(metrics.getStartTime()),
                        ZoneId.systemDefault()
                );

                SimulationRun run = new SimulationRun(
                        engine.getStrategy().getName(),
                        engine.getPhilosopherCount(),
                        startDateTime,
                        metrics.getDurationSec(),
                        metrics.isDeadlockOccurred(),
                        metrics.getTotalMeals(philosophers),
                        mealsJson,
                        metrics.getMaxWaitMs(philosophers),
                        metrics.getFairnessMetric(philosophers)
                );

                runRepository.save(run);
            } catch (JsonProcessingException e) {
                // Ignore serialization error
            }
        }
    }

    private void broadcastEventToEmitters(SimulationEvent event) {
        if (activeEmitters.isEmpty()) return;

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", event);
            payload.put("state", buildCurrentState());
            String json = objectMapper.writeValueAsString(payload);

            for (SseEmitter emitter : activeEmitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("SIM_EVENT")
                            .data(json));
                } catch (Exception ex) {
                    emitter.complete();
                    activeEmitters.remove(emitter);
                }
            }
        } catch (Exception ignored) {}
    }

    public SimulationStateDto buildCurrentState() {
        Table table = engine.getTable();
        List<PhilosopherDto> philosopherDtos = new ArrayList<>();
        List<ForkDto> forkDtos = new ArrayList<>();

        if (table != null) {
            for (Philosopher p : table.getPhilosophers()) {
                philosopherDtos.add(new PhilosopherDto(
                        p.getId(),
                        p.getName(),
                        p.getState().name(),
                        p.getHeldForks(),
                        p.getWaitingForForkId(),
                        p.getMealsEaten(),
                        p.getLongestWaitTimeMs(),
                        p.getLeftFork().getId(),
                        p.getRightFork().getId()
                ));
            }

            for (Fork f : table.getForks()) {
                forkDtos.add(new ForkDto(
                        f.getId(),
                        f.isHeld(),
                        f.getCurrentOwnerId()
                ));
            }
        }

        Metrics metrics = engine.getMetrics();
        List<Philosopher> philosophers = (table != null) ? table.getPhilosophers() : Collections.emptyList();
        boolean deadlocked = engine.getDetector() != null && engine.getDetector().isDeadlocked();
        String cyclePath = (engine.getDetector() != null) ? engine.getDetector().getLastCyclePath() : "";
        List<Integer> cycleNodes = (engine.getDetector() != null) ? engine.getDetector().getLastCycleNodes() : Collections.emptyList();

        return new SimulationStateDto(
                engine.isRunning(),
                deadlocked,
                cyclePath,
                cycleNodes,
                engine.getStrategy().getName(),
                engine.getStrategy().getDescription(),
                engine.getStrategy().getBrokenDeadlockCondition(),
                engine.getPhilosopherCount(),
                engine.getThinkMs(),
                engine.getEatMs(),
                metrics.getDurationSec(),
                metrics.getTotalMeals(philosophers),
                metrics.getMaxWaitMs(philosophers),
                metrics.getFairnessMetric(philosophers),
                philosopherDtos,
                forkDtos
        );
    }

    /**
     * Executes the 5 strategies in isolated test runs for comparison.
     * Naive strategy terminates early when deadlock is detected.
     */
    private List<StrategyComparisonDto> runComparisonBenchmark(int targetDurationSec) {
        String[] strategies = {"naive", "maxfour", "resourceordering", "asymmetric", "waiter"};
        List<StrategyComparisonDto> comparisons = new ArrayList<>();

        for (String stratKey : strategies) {
            SimulationEngine testEngine = new SimulationEngine();
            int pCount = 5;
            int think = 100;
            int eat = 100;

            testEngine.start(stratKey, pCount, think, eat);
            long startMs = System.currentTimeMillis();
            long maxDurationMs = targetDurationSec * 1000L;
            boolean deadlocked = false;

            while (System.currentTimeMillis() - startMs < maxDurationMs) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }

                if (testEngine.getDetector().isDeadlocked()) {
                    deadlocked = true;
                    // If naive deadlocks, stop early to capture actual time to deadlock
                    break;
                }
            }

            testEngine.stop();
            Table testTable = testEngine.getTable();
            List<Philosopher> phils = (testTable != null) ? testTable.getPhilosophers() : Collections.emptyList();
            Metrics m = testEngine.getMetrics();

            comparisons.add(new StrategyComparisonDto(
                    stratKey,
                    testEngine.getStrategy().getName(),
                    testEngine.getStrategy().getBrokenDeadlockCondition(),
                    deadlocked,
                    m.getTotalMeals(phils),
                    m.getMaxWaitMs(phils),
                    m.getFairnessMetric(phils),
                    m.getDurationSec()
            ));
        }

        return comparisons;
    }
}
