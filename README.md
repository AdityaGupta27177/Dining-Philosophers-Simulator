# Dining Philosophers Simulator with Deadlock Detection and Prevention

An interactive, full-stack **Operating Systems Laboratory application** built with **Java 17**, **Maven**, and **Spring Boot 3.2**.

This project models the classic concurrency synchronization dilemma formalized by Edsger Dijkstra and expanded by Tony Hoare. The **core simulation logic is implemented purely in standard Java without any Spring framework annotations**, maintaining strict modularity. Spring Boot is utilized solely for REST API endpoints, real-time Server-Sent Events (SSE), and persistent H2 telemetry.

---

## 🏛️ Syllabus & Theoretical Foundations

### 1. The 4 Coffman Deadlock Conditions (1971)
In Operating Systems theory, a deadlock can occur if and only if all four conditions hold simultaneously:

| Condition | OS Syllabus Definition | Simulator Implementation |
| :--- | :--- | :--- |
| **Mutual Exclusion** | Resources cannot be shared simultaneously; only one thread can hold a resource at a time. | Each `Fork` wraps an exclusive `ReentrantLock(fair = true)`. |
| **Hold and Wait** | A thread holding allocated resources can request additional resources held by others. | A philosopher holds their left fork while waiting to acquire their right fork. |
| **No Preemption** | Resources cannot be forcibly taken from a thread; they must be released voluntarily. | Standard locks cannot be revoked. Breaking this condition is demonstrated via the **Recover** action. |
| **Circular Wait** | A closed chain of threads exists such that each thread waits for a resource held by the next. | $P_0 \to P_1 \to P_2 \to P_3 \to P_4 \to P_0$ in the circular table topology. |

---

## 🍽️ The 5 Dining Strategies

| Strategy | Coffman Condition Broken | Vulnerability | Algorithmic Mechanism |
| :--- | :--- | :--- | :--- |
| **1. Naive Strategy** | **None** | **Deadlocks Reliably** | Every philosopher picks their left fork first, pauses (100ms), and then attempts right fork. All 4 Coffman conditions hold. |
| **2. Max-Four Strategy** | **Hold & Wait / Capacity** | **Deadlock Immune** | Uses a counting `Semaphore(N - 1)` (e.g. 4 diners for 5 forks). By the **Pigeonhole Principle**, at least one diner is guaranteed both forks. |
| **3. Resource Ordering** | **Circular Wait** | **Deadlock Immune** | **Havender’s Principle**: Imposes a global strict total order ($0 < 1 < \dots < N-1$). Always acquires $\min(left, right)$ before $\max(left, right)$. Graph is strictly acyclic. |
| **4. Asymmetric Strategy** | **Circular Wait** | **Deadlock Immune** | **Dijkstra’s Parity Solution**: Odd-numbered philosophers pick right then left; even pick left then right. Symmetrical circular contention is broken. |
| **5. Central Waiter** | **Hold and Wait** | **Deadlock Immune** | A central monitor (`ReentrantLock` + `Condition`) allocates both forks atomically or none. A diner never holds 1 fork while waiting. |

---

## 🔍 Deadlock Detection & Preemption Recovery

### 1. Wait-For Graph (WFG) & DFS Cycle Detection
The `DeadlockDetector` runs on a dedicated background thread:
1. Examines all philosophers in the `BLOCKED` state.
2. Identifies the fork being requested and the philosopher currently holding it.
3. Constructs directed edges: $P_i \xrightarrow{waiting\ for} P_j$.
4. Runs **Depth-First Search (DFS)** with recursion stack tracking to locate closed cycles (e.g., `P0 -> P1 -> P2 -> P3 -> P4 -> P0`).
5. Publishes a `DEADLOCK_DETECTED` event and automatically saves the run telemetry to the H2 database.

### 2. Breaking No-Preemption (The "Recover" Action)
When deadlocked, the **Recover** button:
1. Selects a "victim" thread from the cycle (the philosopher with the lowest meals eaten).
2. Forcibly preempts the victim's held fork via cooperative interrupt and lock revocation.
3. The freed fork is acquired by the waiting adjacent philosopher, resolving the cycle and demonstrating **preemption-based recovery**.

---

## 📁 Package Architecture

```
com.diningphilosophers
├── DiningPhilosophersApplication.java    # Spring Boot entry point
├── core/                                 # 100% Plain Java (No Spring Annotations!)
│   ├── Fork.java                         # ReentrantLock wrapper with mutual exclusion & owner tracking
│   ├── Philosopher.java                  # Runnable thread lifecycle (Think -> Hungry -> Eat -> Release)
│   ├── PhilosopherState.java             # Enum: THINKING, HUNGRY, EATING, BLOCKED
│   ├── Table.java                        # Circular topology management for N philosophers & forks
│   ├── SimulationEngine.java             # ExecutorService thread management & clean lifecycle
│   ├── EventBus.java                     # Thread-safe pub-sub event bus
│   ├── SimulationEvent.java              # Immutable timestamped event
│   ├── EventType.java                    # Event type definitions
│   └── Metrics.java                      # Throughput, wait times, and fairness tracking
├── strategy/                             # Strategy implementations
│   ├── DiningStrategy.java               # Interface for fork acquisition protocols
│   ├── NaiveStrategy.java                # Deadlock-prone naive implementation
│   ├── MaxFourStrategy.java              # Semaphore(N-1) diner limiter
│   ├── ResourceOrderingStrategy.java     # Havender's resource hierarchy
│   ├── AsymmetricStrategy.java           # Odd-right, Even-left symmetry breaker
│   └── WaiterStrategy.java               # Central monitor all-or-nothing allocation
├── detection/                            # Cycle detection
│   ├── DeadlockDetector.java             # Dedicated detection thread
│   └── WaitForGraph.java                 # Directed graph & DFS cycle search
├── recovery/                             # Preemption recovery
│   └── DeadlockRecovery.java             # Forcible resource revocation module
├── persistence/                          # Spring Data JPA + H2 (File-backed)
│   ├── SimulationRun.java                # JPA Entity storing run statistics
│   └── SimulationRunRepository.java      # Spring Data JPA Repository
└── web/                                  # Spring Web REST + SSE
    ├── SimulationController.java         # REST Controller & SSE Emitter
    ├── StartRequest.java                 # Request payload DTO
    ├── SimulationStateDto.java           # State snapshot DTO
    ├── PhilosopherDto.java               # Philosopher view DTO
    ├── ForkDto.java                      # Fork view DTO
    └── StrategyComparisonDto.java        # Strategy benchmark DTO
```

---

## 🚀 Windows Quickstart Guide (Java 17 & Maven)

### Prerequisites
- **Java Development Kit (JDK) 17** or newer installed.
- **Apache Maven 3.8+** installed.

### 1. Run with Maven
Open PowerShell or Command Prompt in the project root directory:

```powershell
mvn clean spring-boot:run
```

### 2. Open the Web Application
Open your web browser and navigate to:
```
http://localhost:8080
```

### 3. Access H2 Database Console
The embedded file-backed database is accessible at:
- **URL**: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:file:./data/dining_db`
- **User Name**: `sa`
- **Password**: *(leave blank)*

---

## 📡 REST API Reference

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/start` | Starts the simulation with JSON `{ "strategy": "naive", "philosophers": 5, "thinkMs": 400, "eatMs": 400 }` |
| `POST` | `/api/stop` | Stops all active philosopher threads cleanly and saves telemetry |
| `POST` | `/api/reset` | Resets the table, clearing metrics, forks, and events |
| `POST` | `/api/recover` | Forcibly preempts a held fork from a victim philosopher to resolve deadlock |
| `GET` | `/api/state` | Returns the current JSON snapshot of the table, philosophers, forks, and metrics |
| `GET` | `/api/events` | **Server-Sent Events (SSE)** stream delivering live events and state heartbeats |
| `GET` | `/api/history` | Returns the database history of past runs from H2 |
| `GET` | `/api/compare` | Runs all 5 strategies under 15-second benchmark contention and returns comparison data |

---

## 🧪 Automated JUnit 5 Concurrency Tests

Run the test suite via Maven:
```powershell
mvn test
```

The test suite includes:
1. `NaiveStrategyTest`: Verifies that `NaiveStrategy` reliably causes a deadlock within the timeout and outputs the expected cycle path.
2. `MaxFourStrategyTest`: Verifies that `MaxFourStrategy` runs without deadlocking and philosophers continuously consume meals.
3. `ResourceOrderingStrategyTest`: Verifies that `ResourceOrderingStrategy` runs without deadlocking under high contention.
