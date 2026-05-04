# Day 29: Circuit Breaker, Failure Thresholds, and Recovery Probing

Day 29 focuses on preventing cascading failures when downstream dependencies are failing or slow.

It covers:
- circuit breaker fundamentals
- closed/open/half-open states
- failure threshold and cooldown windows
- probe requests for dependency recovery
- fallback and fail-fast behavior
- reliability trade-offs and tuning

In short, Day 29 is about cutting off repeated failing calls quickly and restoring traffic safely when a dependency recovers.

![Day 29](./Day29.png)

[Day 29 PDF](./System_Design_Day_29.pdf)

## Core ideas

### Why circuit breakers matter
- Repeated calls to a failing dependency waste threads and increase latency.
- Without protection, failures propagate upstream and can trigger wider outages.
- Circuit breakers fail fast during dependency incidents and protect service capacity.

### State machine model
- `CLOSED`: normal mode; calls pass through and failures are counted.
- `OPEN`: calls are blocked immediately after failure threshold is exceeded.
- `HALF_OPEN`: after cooldown, limited probe calls test whether dependency recovered.

### Failure thresholds and cooldown
- Breaker opens after a configured number of recent failures.
- Cooldown timer prevents immediate retry storms.
- After cooldown, a probe decides whether to close breaker or reopen.

### Recovery and fallback
- In `OPEN`, system should return fallback/fast error instead of waiting on dependency timeouts.
- Successful probe in `HALF_OPEN` closes breaker and restores normal traffic.
- Failed probe reopens breaker and restarts cooldown.

### Trade-offs
- Aggressive thresholds improve protection but can trip too often.
- Lenient thresholds reduce false trips but allow more failing traffic.
- Tuning depends on dependency SLO, latency budgets, and error budgets.

## Day-29 sample: Circuit Breaker Dependency Guard

This repository includes a small circuit breaker sample in both Python and Java.

### Functional requirements
- Wrap downstream calls with a circuit breaker
- Track failures and open after threshold
- Block calls while open
- Transition to half-open after cooldown ticks
- Close on successful probe or reopen on failed probe
- Report request outcomes and breaker state over time

### High-level components
- `CircuitBreaker`: state machine and thresholds
- `DependencySimulator`: scripted success/failure outcomes
- `GuardedClient`: call wrapper with fallback behavior
- `Metrics`: counters for success, failure, blocked, and state transitions

### Data flow
1. Client attempts downstream call through breaker
2. Breaker decides allow/block based on state
3. If allowed, dependency result updates breaker state
4. On threshold breach, breaker opens and starts cooldown
5. After cooldown, half-open probe determines recovery path

## Project structure

```text
Day-29/
  README.md
  circuit-breaker-sample/
    python/
      circuit_breaker.py
      demo.py
    java/
      CircuitBreakerSystem.java
      Main.java
```

## Run the sample

### Python

```powershell
cd circuit-breaker-sample\python
python demo.py
```

### Java

```powershell
cd circuit-breaker-sample\java
javac Main.java CircuitBreakerSystem.java
java Main
```

## Interview takeaways
- Circuit breakers are a core anti-cascading-failure mechanism
- Open-state fail-fast protects latency and thread pools
- Half-open probing enables controlled recovery
- Threshold and cooldown tuning must be data-driven
- Breakers complement retries, backpressure, and bulkheads

## Next improvements
- Add rolling-window failure rate thresholds
- Add separate handling for timeout vs functional errors
- Add per-endpoint breakers and adaptive cooldown
- Add fallback response strategies by error class
- Add SLO-based alerting for breaker open duration
