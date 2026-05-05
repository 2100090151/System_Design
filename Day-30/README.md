# Day 30: Timeouts, Deadlines, and Cancellation Propagation

Day 30 focuses on how distributed systems bound latency and stop wasted work when downstream calls are too slow.

It covers:
- timeout and deadline fundamentals
- per-hop latency budgeting
- cancellation propagation across service chains
- fail-fast behavior under budget exhaustion
- partial-work waste reduction
- reliability and UX trade-offs

In short, Day 30 is about enforcing time limits consistently so slow dependencies do not consume resources indefinitely.

## Core ideas

### Why timeouts matter
- Slow dependencies can hold threads, sockets, and queues for too long.
- Without limits, one slow path can degrade unrelated traffic.
- Timeouts bound tail latency and protect service capacity.

### Timeout vs deadline
- `Timeout`: max wait for one operation.
- `Deadline`: absolute remaining budget across the full request.
- Deadline-aware services avoid spending more than the end-to-end latency budget.

### Budgeting across hops
- Request budget should be split across downstream calls.
- Each hop checks remaining budget before starting work.
- If budget is exhausted, the call should fail fast.

### Cancellation propagation
- When a request times out upstream, downstream work should be cancelled.
- Propagated cancellation reduces wasted compute and lock contention.
- Cancellation signals should be explicit in inter-service contracts.

### Trade-offs
- Tight timeouts improve responsiveness but may increase false failures.
- Loose timeouts reduce false failures but increase resource pressure.
- Timeout tuning must align with SLO targets and dependency behavior.

## Day-30 sample: Deadline Guarded Request Chain

This repository includes a small timeout/deadline sample in both Python and Java.

### Functional requirements
- Execute a request through multiple service steps
- Enforce one shared deadline budget
- Fail fast when remaining budget is insufficient
- Mark downstream steps as cancelled after timeout
- Report timeline and final status (`SUCCESS` or `TIMEOUT`)

### High-level components
- `RequestContext`: deadline, elapsed time, cancellation state
- `ServiceStep`: simulated step with fixed processing duration
- `DeadlineExecutor`: orchestrates chain execution with budget checks
- `ExecutionResult`: status and event timeline

### Data flow
1. Request starts with total deadline budget
2. Executor checks remaining time before each step
3. Step consumes time from budget
4. On budget exhaustion, executor marks timeout and propagates cancellation
5. Result returns status and per-step timeline for debugging

## Project structure

```text
Day-30/
  README.md
  timeout-control-sample/
    python/
      timeout_control.py
      demo.py
    java/
      TimeoutControlSystem.java
      Main.java
```

## Run the sample

### Python

```powershell
cd timeout-control-sample\python
python demo.py
```

### Java

```powershell
cd timeout-control-sample\java
javac Main.java TimeoutControlSystem.java
java Main
```

## Interview takeaways
- Deadlines should be propagated end-to-end, not managed independently per hop
- Cancellation is part of correctness and capacity protection
- Timeout policy affects both latency and error rate
- Fail-fast behavior limits cascading saturation under slow dependencies
- Observability needs timeout/cancel metrics and per-hop timing

## Next improvements
- Add adaptive timeout tuning from historical latency percentiles
- Add retries with remaining-budget awareness
- Add hedged requests for high-tail dependencies
- Add separate connect/read timeout modeling
- Add tracing IDs and per-hop latency histograms
