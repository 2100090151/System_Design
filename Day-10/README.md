# Day 10: Queues, Retries, and Dead-Letter Handling

Day 10 focuses on how systems process asynchronous work safely when downstream services fail or become slow.

It covers:
- asynchronous job queues
- producer/consumer decoupling
- at-least-once delivery and duplicate handling
- retries with bounded attempts
- dead-letter queue (DLQ) for poison messages
- backpressure and queue capacity control

In short, Day 10 is about making background processing resilient without blocking user-facing request paths.

![Day 10](./Day10.png)

[Day 10 PDF](./System_Design_Day_10.pdf)

## Core ideas

### Why queues matter
- A `queue` lets producers hand off work and return quickly.
- Consumers can process jobs at their own speed.
- This decoupling smooths traffic spikes and protects core systems from bursts.

### Delivery guarantees and idempotency
- Most practical queue systems provide `at-least-once` delivery.
- At-least-once means duplicates are possible, so handlers must be idempotent.
- Exactly-once is expensive and usually replaced by idempotent processing plus dedup keys.

### Retries and DLQ
- Transient failures should be retried with bounded attempts.
- Permanent failures should move to a `dead-letter queue` for inspection.
- DLQ prevents one bad message from blocking healthy traffic.

### Backpressure
- If producers are faster than consumers, queue length grows.
- Systems need queue limits, admission control, or load shedding.
- Backpressure keeps downstream services and workers stable under load.

## Day-10 sample: Retry Queue with Dead-Letter Handling

This repository includes a small retry queue sample in both Python and Java.

### Functional requirements
- Enqueue jobs with bounded queue capacity
- Poll jobs for processing
- Acknowledge successful jobs
- Retry failed jobs up to a configured maximum
- Move exhausted jobs to dead-letter storage

### High-level components
- `Producer`: submits jobs into the queue
- `Worker`: polls jobs and processes them
- `In-flight map`: tracks currently delivered jobs awaiting ack/fail
- `Retry logic`: requeues failed jobs until max retries
- `Dead-letter list`: stores jobs that exceeded retry budget

### Data flow
1. Producer adds jobs to the queue
2. Worker polls one job and starts processing
3. On success, worker acknowledges and job is removed
4. On failure, queue increments attempt count
5. If attempts stay within limit, job is requeued
6. If retry budget is exhausted, job is moved to dead-letter storage

## Project structure

```text
Day-10/
  README.md
  retry-queue-sample/
    python/
      retry_queue.py
      demo.py
    java/
      RetryQueue.java
      Main.java
```

## Run the sample

### Python

```powershell
cd retry-queue-sample\python
python demo.py
```

### Java

```powershell
cd retry-queue-sample\java
javac Main.java RetryQueue.java
java Main
```

## Interview takeaways
- Queues decouple request paths from slow background work
- At-least-once delivery requires idempotent consumers
- Retries need a cap to avoid infinite poison-message loops
- DLQs are critical for operability and debugging
- Backpressure controls are part of correctness, not just performance

## Next improvements
- Add exponential backoff with jitter between retries
- Add delayed queues for scheduled retry attempts
- Add per-job deduplication keys
- Add metrics for queue depth, retries, and DLQ rate
- Replace in-memory storage with Redis, SQS, RabbitMQ, or Kafka
