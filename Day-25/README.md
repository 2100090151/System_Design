# Day 25: Inbox Pattern, Exactly-Once Effect, and Consumer Reliability

Day 25 focuses on how consumers achieve exactly-once business effect over at-least-once delivery by combining idempotent processing with a durable inbox record.

It covers:
- inbox pattern fundamentals
- durable deduplication at consumer boundary
- atomic consume + apply workflow
- retry-safe processing semantics
- poison message handling strategy
- operational trade-offs for reliability

In short, Day 25 is about making each logical message affect business state once, even when delivery is repeated.

![Day 25](./Day25.png)

[Day 25 PDF](./System_Design_Day_25.pdf)

## Core ideas

### Why Inbox pattern exists
- Message brokers often provide at-least-once delivery, not exactly-once processing.
- In-memory dedup can fail on restarts and lose seen-message history.
- Inbox pattern stores processed message IDs durably at consumer side.

### Consumer-side atomicity
- Consumer reads message and checks inbox table/store for `message_id`.
- If absent, it applies business update and records inbox entry in one transaction.
- If present, it skips side effects and acknowledges safely.

### Exactly-once effect vs exactly-once delivery
- Delivery may happen multiple times.
- Inbox guarantees side effects are applied once per logical message.
- This is usually the practical target in distributed systems.

### Failure behavior
- If processing crashes before commit, message can be retried safely.
- If commit succeeds but ack fails, redelivery is skipped due to inbox record.
- Poison messages should be routed to DLQ after bounded retries.

### Trade-offs
- Inbox improves correctness with additional storage and write cost.
- Large throughput systems need retention policies for inbox entries.
- Key design and transactional boundaries are critical for correctness.

## Day-25 sample: Durable Inbox Payment Consumer

This repository includes a small inbox-pattern sample in both Python and Java.

### Functional requirements
- Consume payment events with unique `message_id`
- Apply account ledger update once per unique message
- Persist processed IDs in durable inbox store
- Skip duplicates safely across repeated deliveries
- Expose processing metrics (`applied`, `duplicate`)

### High-level components
- `Message`: event envelope
- `InboxStore`: durable processed-message registry
- `LedgerStore`: business side-effect state
- `InboxConsumer`: atomic process-and-record handler

### Data flow
1. Broker delivers message
2. Consumer checks inbox for `message_id`
3. If unseen, consumer applies ledger update and records inbox entry
4. If seen, consumer skips ledger write
5. Metrics show applied vs duplicate counts

## Project structure

```text
Day-25/
  README.md
  inbox-pattern-sample/
    python/
      inbox_consumer.py
      demo.py
    java/
      InboxConsumerSystem.java
      Main.java
```

## Run the sample

### Python

```powershell
cd inbox-pattern-sample\python
python demo.py
```

### Java

```powershell
cd inbox-pattern-sample\java
javac Main.java InboxConsumerSystem.java
java Main
```

## Interview takeaways
- Inbox pattern is a consumer-side complement to producer outbox
- Durable deduplication survives consumer restarts and retries
- Exactly-once effect is usually more realistic than exactly-once delivery
- Transaction boundaries must include both business update and inbox insert
- Metrics and DLQ policy are necessary for production reliability

## Next improvements
- Add TTL/archival policy for old inbox records
- Use database unique constraints for message IDs
- Add retry backoff with DLQ routing simulation
- Add partition-aware consumer groups and ordering guarantees
- Add tracing IDs for end-to-end duplicate diagnosis
