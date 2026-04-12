# Day 7: Observability, Monitoring, and Tracing

Day 7 focuses on how distributed systems are measured, debugged, and understood once they are running in production.

It covers:
- logs, metrics, and traces
- structured logging and correlation IDs
- white-box vs black-box monitoring
- RED and USE metrics
- dashboards, alerting, and incident response
- distributed tracing and context propagation
- sampling, cardinality, and telemetry cost trade-offs

In short, Day 7 is about making production systems understandable enough to detect failures quickly and debug them without guesswork.

![Day 7](./Day7.png)

## Core ideas

### Logs, metrics, and traces
- `Logs` capture detailed events and are useful for debugging specific failures.
- `Metrics` summarize system behavior over time and are useful for dashboards and alerts.
- `Traces` connect work across services and help explain end-to-end latency.

### Structured logging and correlation IDs
- `Structured logs` make machine filtering and aggregation easier than free-form log lines.
- `Correlation IDs` and `trace IDs` tie related events together across services.
- Without consistent request identifiers, debugging a distributed request becomes slow and error-prone.

### Monitoring models and service health
- `Black-box monitoring` checks the externally visible behavior of a service.
- `White-box monitoring` inspects internals like queue depth, CPU, retries, and cache hit rate.
- Both are necessary because a service can look healthy internally while failing from the user's perspective.

### RED, USE, and alerting
- `RED` stands for request rate, error rate, and duration.
- `USE` stands for utilization, saturation, and errors.
- Good alerts are actionable, low-noise, and tied to user impact or important resource limits.

### Distributed tracing and telemetry trade-offs
- `Distributed tracing` follows one request across multiple services using propagated context.
- `Sampling` reduces telemetry cost, but can hide rare failures if configured badly.
- High-cardinality labels improve diagnosis, but they also increase storage and query cost.

## Day-7 sample: Trace Context Propagator

This repository includes a small trace context propagation sample in both Python and Java.

### Functional requirements
- Start a root trace for an incoming request
- Propagate trace headers between services
- Create a child span for each downstream call
- Emit correlated log lines that share the same trace ID

### High-level components
- `Client`: initiates a request
- `Gateway`: starts the root trace
- `Service`: extracts incoming trace context and creates child spans
- `Headers`: carry trace identifiers between services
- `Logs`: expose the trace and span IDs for debugging

### Data flow
1. A client request reaches the gateway
2. The gateway creates a root trace and logs the request
3. Trace headers are injected into the outbound call to the next service
4. The receiving service extracts the trace context and creates a child span
5. Every service logs with the same trace ID so the full path can be reconstructed

## Project structure

```text
Day-7/
  README.md
  trace-context-sample/
    python/
      trace_context.py
      demo.py
    java/
      TraceContext.java
      Main.java
```

## Run the sample

### Python

```powershell
cd trace-context-sample\python
python demo.py
```

### Java

```powershell
cd trace-context-sample\java
javac Main.java TraceContext.java
java Main
```

## Interview takeaways
- You cannot operate a distributed system reliably if you cannot correlate logs, metrics, and traces
- RED metrics are a strong default starting point for user-facing services
- Distributed tracing is valuable only if context propagation is consistent across every hop
- Alert quality matters more than alert quantity
- Telemetry is a product cost; sample and label carefully

## Next improvements
- Add W3C `traceparent` header support
- Add span timing and latency measurement
- Export traces to OpenTelemetry-compatible format
- Add sampling policies for high-volume traffic
- Extend the sample into a small multi-service HTTP demo
