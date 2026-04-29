# Day 24: Idempotent Consumer, Deduplication, and Delivery Guarantees

Day 24 focuses on how consumers process messages safely when brokers deliver duplicates due to retries and at-least-once semantics.

It covers:
- at-least-once delivery behavior
- idempotent consumer design
- deduplication key strategy
- side-effect safety under retries
- poison message and retry trade-offs
- operational observability for duplicate processing

In short, Day 24 is about making consumer processing safe to repeat without corrupting state.

![Day 24](./Day24.png)

[Day 24 PDF](./System_Design_Day_24.pdf)

## Core ideas

### Why duplicates happen
- Network failures and ack timeouts can trigger message redelivery.
- Producers and brokers may retry the same logical message.
- At-least-once delivery prioritizes durability over uniqueness.

### Idempotent consumer principle
- Processing the same message multiple times should produce one effective business outcome.
- Consumer stores a stable dedup key (for example, `message_id` or business operation id).
- Already-seen keys are skipped before side effects run.

### Deduplication design
- Dedup store can be in-memory, cache, or durable database table.
- Retention/TTL must cover expected replay and retry windows.
- Key choice should map to business uniqueness, not transient transport metadata.

### Failure handling
- If side effect succeeds but ack fails, duplicate arrives later.
- Idempotency prevents charging, emailing, or updating inventory twice.
- Poison messages should be routed to dead-letter handling after bounded retries.

### Trade-offs
- Strong dedup improves correctness but adds storage and coordination overhead.
- Very short TTL can reintroduce duplicates after expiry.
- Exactly-once is usually end-to-end expensive; idempotency is the practical baseline.

## Day-24 sample: Idempotent Payment Event Consumer

This repository includes a small idempotent consumer sample in both Python and Java.

### Functional requirements
- Consume payment events with `message_id` and amount
- Apply account balance side effect once per unique message
- Skip duplicate deliveries safely
- Track processing audit counters (applied vs skipped)
- Demonstrate duplicate-heavy input stream handling

### High-level components
- `Message`: immutable event envelope
- `DedupStore`: remembers processed message IDs
- `BalanceLedger`: side-effect target state
- `PaymentConsumer`: idempotent message handler and metrics

### Data flow
1. Broker delivers a message to consumer
2. Consumer checks dedup store by `message_id`
3. If seen, consumer skips side effect and records duplicate metric
4. If new, consumer applies ledger update and marks ID as processed
5. Metrics report applied/skipped totals for observability

## Project structure

```text
Day-24/
  README.md
  idempotent-consumer-sample/
    python/
      idempotent_consumer.py
      demo.py
    java/
      IdempotentConsumerSystem.java
      Main.java
```

## Run the sample

### Python

```powershell
cd idempotent-consumer-sample\python
python demo.py
```

### Java

```powershell
cd idempotent-consumer-sample\java
javac Main.java IdempotentConsumerSystem.java
java Main
```

## Interview takeaways
- At-least-once delivery requires idempotent consumers to preserve correctness
- Dedup keys should represent business uniqueness and survive retries
- Consumer metrics should expose duplicate rate and processing outcomes
- Dead-letter and retry strategy must be explicit in design discussions
- Idempotency is often more practical than strict exactly-once guarantees

## Next improvements
- Add TTL-based dedup retention policy
- Persist dedup keys in durable storage
- Add retry backoff and DLQ simulation
- Add per-partition ordering behavior and keying strategy
- Add tracing and audit logs for replay investigations
