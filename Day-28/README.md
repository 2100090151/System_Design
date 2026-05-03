# Day 28: Bulkhead Isolation, Worker Pools, and Blast Radius Control

Day 28 focuses on isolating workloads so one failing dependency or noisy traffic class cannot consume all system resources.

It covers:
- bulkhead isolation fundamentals
- dedicated worker pool strategy
- per-pool queue limits
- overload containment behavior
- graceful degradation patterns
- operational trade-offs and observability

In short, Day 28 is about containing failures to one compartment instead of letting them sink the entire service.

![Day 28](./Day28.png)

[Day 28 PDF](./System_Design_Day_28.pdf)

## Core ideas

### Why bulkheads matter
- Shared thread pools and queues create coupling between unrelated workloads.
- A slow downstream call can saturate common resources and block critical paths.
- Bulkheads isolate resources by traffic class or dependency.

### Isolation model
- Assign dedicated pool/queue to each workload lane (for example `critical` vs `best_effort`).
- Capacity limits are independent per lane.
- Failures and backlog stay local to the overloaded lane.

### Degradation behavior
- Non-critical lane can throttle/shed while critical lane remains healthy.
- Critical lane gets reserved capacity and lower tail-latency risk.
- Service can continue partial functionality during incidents.

### Operational signals
- Per-lane queue depth, rejection count, and completion latency are key.
- Saturation in one lane should not imply global outage.
- Alerting should be pool-aware to avoid noisy incident triage.

### Trade-offs
- Isolation increases reliability but may reduce average utilization.
- Over-provisioned pools waste capacity; under-provisioned pools shed too aggressively.
- Pool sizing should follow workload criticality and SLO targets.

## Day-28 sample: Bulkhead Worker Pool Simulator

This repository includes a small bulkhead simulation in both Python and Java.

### Functional requirements
- Route tasks to dedicated pools by lane (`critical`, `best_effort`)
- Enforce independent queue limits per lane
- Process tasks at fixed throughput per lane
- Reject overflow tasks per lane without affecting the other lane
- Report per-lane accepted/processed/rejected metrics

### High-level components
- `Task`: unit of work tagged with lane
- `BulkheadPool`: lane-specific bounded queue and worker throughput
- `BulkheadRouter`: dispatches tasks to the correct pool
- `BulkheadEngine`: tick-based simulation and metrics aggregation

### Data flow
1. Incoming task arrives with lane tag
2. Router forwards task to matching pool
3. Pool accepts or rejects based on lane queue capacity
4. Worker drains fixed number of tasks from each lane per tick
5. Engine reports isolated lane-level metrics

## Project structure

```text
Day-28/
  README.md
  bulkhead-sample/
    python/
      bulkhead_isolation.py
      demo.py
    java/
      BulkheadSystem.java
      Main.java
```

## Run the sample

### Python

```powershell
cd bulkhead-sample\python
python demo.py
```

### Java

```powershell
cd bulkhead-sample\java
javac Main.java BulkheadSystem.java
java Main
```

## Interview takeaways
- Bulkheads reduce blast radius during localized failures
- Resource isolation should align with business criticality
- Lane-level metrics are required to tune capacity safely
- Graceful degradation is better than full-service collapse
- Bulkheads pair well with retries, backpressure, and circuit breakers

## Next improvements
- Add dynamic pool sizing by queue pressure
- Add lane priorities and admission control hints
- Add timeout/cancellation handling per lane
- Add dependency-specific bulkheads (DB, cache, third-party API)
- Add SLO-driven autoscaling based on pool saturation
