# Day 26: Retry Backoff, Dead Letter Queue, and Poison Message Handling

Day 26 focuses on how consumers recover from transient failures, cap retry attempts, and isolate poison messages without blocking healthy traffic.

It covers:
- retry policy fundamentals
- fixed/exponential backoff behavior
- max-attempt thresholds
- dead letter queue (`DLQ`) routing
- poison message isolation
- operational visibility and trade-offs

In short, Day 26 is about keeping event processing resilient when some messages repeatedly fail.

![Day 26](./Day26.png)

[Day 26 PDF](./System_Design_Day_26.pdf)

## Core ideas

### Why retries are needed
- Many failures are transient (network blips, temporary dependency overload).
- Immediate permanent failure increases data loss risk.
- Retrying improves success rate for temporary issues.

### Backoff strategy
- Backoff spaces retries to avoid hammering failing dependencies.
- Fixed backoff is simple; exponential backoff is safer under sustained failures.
- Jitter is often added in production to prevent retry storms.

### Max attempts and DLQ
- Some messages are poison (invalid data or persistent business conflict).
- After bounded attempts, route message to DLQ.
- DLQ prevents one bad message from blocking throughput.

### Operational flow
- Main consumer processes messages and retries on failure.
- Failed messages carry attempt count metadata.
- Alerting/triage pipeline inspects DLQ messages for remediation.

### Trade-offs
- More retries can improve recovery but increase latency and cost.
- Too few retries can lose recoverable work.
- DLQ requires ownership, replay tooling, and observability.

## Day-26 sample: Retry Queue with Dead Letter Routing

This repository includes a small retry + DLQ simulation in both Python and Java.

### Functional requirements
- Process messages from a primary queue
- Retry failed messages with bounded attempts
- Re-enqueue retryable failures with incremented attempt count
- Move exhausted messages to DLQ
- Report success/retry/DLQ metrics

### High-level components
- `WorkMessage`: message envelope with attempt count
- `Worker`: business processor with simulated transient/permanent failures
- `RetryEngine`: retry loop and attempt accounting
- `DeadLetterQueue`: terminal storage for exhausted messages

### Data flow
1. Engine consumes message
2. Worker attempts processing
3. On success, message is acknowledged
4. On failure with attempts remaining, message is retried
5. On failure with max attempts reached, message is moved to DLQ

## Project structure

```text
Day-26/
  README.md
  dlq-retry-sample/
    python/
      retry_dlq.py
      demo.py
    java/
      RetryDlqSystem.java
      Main.java
```

## Run the sample

### Python

```powershell
cd dlq-retry-sample\python
python demo.py
```

### Java

```powershell
cd dlq-retry-sample\java
javac Main.java RetryDlqSystem.java
java Main
```

## Interview takeaways
- Retries should target transient failures with bounded policy
- Backoff and jitter reduce cascading retry pressure
- DLQ is a reliability control, not just an error bucket
- Poison message isolation protects overall system throughput
- Observability of retries and DLQ volume is mandatory in production

## Next improvements
- Add exponential backoff with jitter simulation
- Add error classification (retryable vs non-retryable)
- Add DLQ replay workflow with remediation hook
- Add per-tenant or per-partition retry isolation
- Add SLO metrics for retry latency and DLQ rate
