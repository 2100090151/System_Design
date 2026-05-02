# Day 27: Backpressure, Flow Control, and Load Shedding

Day 27 focuses on how systems stay stable when incoming traffic exceeds processing capacity.

It covers:
- backpressure fundamentals
- bounded queue flow control
- lag and saturation signals
- intake throttling strategies
- load shedding under overload
- reliability and latency trade-offs

In short, Day 27 is about protecting the system from collapse by controlling admission when demand spikes.

![Day 27](./Day27.png)

[Day 27 PDF](./System_Design_Day_27.pdf)

## Core ideas

### Why backpressure matters
- Producers can generate work faster than consumers can process it.
- Without control, queues grow unbounded and latency explodes.
- Backpressure keeps the system within safe operating limits.

### Bounded queues and lag
- A queue capacity cap is a hard safety boundary.
- Queue depth and lag are primary indicators of overload.
- Once depth crosses thresholds, the system should reduce intake or shed load.

### Throttling and shedding
- `Throttling` slows admission to match downstream capacity.
- `Load shedding` rejects lower-priority or excess work to preserve core throughput.
- These are resilience tools, not failure states.

### Operational behavior
- Overload handling should be explicit and measurable.
- Metrics like `accepted`, `processed`, `shed`, and `queue_depth` guide tuning.
- Policies should be deterministic to simplify incident analysis.

### Trade-offs
- Aggressive shedding improves stability but drops more work.
- Lenient admission reduces drops but risks long latency and timeout cascades.
- Policy tuning depends on business SLAs and workload criticality.

## Day-27 sample: Backpressure Admission Controller

This repository includes a small backpressure simulation in both Python and Java.

### Functional requirements
- Accept incoming work into a bounded queue
- Process work at fixed consumer throughput per tick
- Apply watermark-based admission policy
- Throttle intake near saturation
- Shed load at high queue pressure
- Report operational metrics and lag trend

### High-level components
- `WorkItem`: unit of queued work
- `BoundedQueue`: capacity-limited in-memory queue
- `AdmissionController`: watermark policy for accept/throttle/shed
- `Consumer`: fixed-rate processor
- `BackpressureEngine`: tick-based simulation and metrics

### Data flow
1. Incoming batch arrives for a simulation tick
2. Admission controller decides per item (accept/throttle/shed)
3. Accepted items enter bounded queue
4. Consumer drains up to configured throughput
5. Metrics and queue depth are recorded for tuning analysis

## Project structure

```text
Day-27/
  README.md
  backpressure-sample/
    python/
      backpressure_controller.py
      demo.py
    java/
      BackpressureSystem.java
      Main.java
```

## Run the sample

### Python

```powershell
cd backpressure-sample\python
python demo.py
```

### Java

```powershell
cd backpressure-sample\java
javac Main.java BackpressureSystem.java
java Main
```

## Interview takeaways
- Backpressure prevents overload from becoming a full outage
- Queue depth is both a control input and an SLO risk signal
- Throttling and shedding should be policy-driven and observable
- Stability often requires rejecting some work under bursty load
- Reliability design includes graceful degradation, not just retries

## Next improvements
- Add request priority classes for selective shedding
- Add adaptive consumer scaling policy
- Add exponential backoff hints to producers
- Add end-to-end latency percentile tracking
- Add alert thresholds for sustained high queue depth
