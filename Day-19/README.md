# Day 19: Transactional Outbox, Dual-Write Safety, and Reliable Event Delivery

Day 19 focuses on how distributed systems safely publish events when business state changes, without losing updates or emitting ghost events.

It covers:
- dual-write failure mode fundamentals
- transactional outbox pattern
- relay/poller based event publication
- at-least-once delivery and retry behavior
- idempotency implications for consumers
- ordering and throughput trade-offs

In short, Day 19 is about guaranteeing that "state change" and "event intent" are stored atomically, then published reliably.

![Day 19](./Day19.png)

[Day 19 PDF](./System_Design_Day_19.pdf)

## Core ideas

### Why dual writes fail
- Writing to the database and broker in two separate operations can diverge.
- If DB commit succeeds but broker publish fails, downstream systems miss the event.
- If broker publish succeeds but DB commit fails, downstream systems see an event for data that does not exist.

### Transactional outbox pattern
- Service writes domain state and an outbox row in one local DB transaction.
- If the transaction commits, both records exist.
- If it aborts, neither exists.

### Relay/poller role
- A separate relay process scans pending outbox rows.
- It publishes them to the broker and marks them as sent only after acknowledgement.
- Failed publishes remain pending and are retried.

### Delivery semantics
- Outbox relay typically gives at-least-once delivery.
- Duplicate events are possible during retries and crash recovery.
- Consumers should be idempotent and use event IDs for deduplication.

### Trade-offs
- Outbox removes dual-write inconsistency for service-owned state.
- It adds operational components (relay, retention, monitoring).
- Ordering is easiest per aggregate/partition key, harder globally.

## Day-19 sample: Transactional Outbox Simulation

This repository includes a small transactional outbox sample in both Python and Java.

### Functional requirements
- Create an order and outbox event in one local transaction
- Ensure failed transactions do not leak events
- Publish pending outbox events through a relay
- Retry failed publishes without losing messages
- Mark messages as `SENT` only after publish success

### High-level components
- `InMemoryStore`: domain state and outbox storage
- `Transaction`: stages order + event and commits atomically
- `OutboxRelay`: polls pending messages and publishes them
- `UnreliableBroker`: simulates publish failures for retry demonstration

### Data flow
1. Service starts a transaction
2. Service stages domain write and outbox message
3. On commit success, both are persisted together
4. Relay polls pending outbox messages
5. Relay publishes to broker
6. On success, message is marked sent; on failure, attempts increment and message stays pending

## Project structure

```text
Day-19/
  README.md
  outbox-sample/
    python/
      outbox_pattern.py
      demo.py
    java/
      TransactionalOutbox.java
      Main.java
```

## Run the sample

### Python

```powershell
cd outbox-sample\python
python demo.py
```

### Java

```powershell
cd outbox-sample\java
javac Main.java TransactionalOutbox.java
java Main
```

## Interview takeaways
- Transactional outbox is the standard fix for service-level dual-write inconsistency
- Relay failures should not delete events; retries preserve eventual delivery
- At-least-once delivery means idempotent consumers are mandatory
- Monitoring pending outbox backlog is a key operational SLI
- Outbox pairs well with Saga, CQRS, and event-driven architectures

## Next improvements
- Add partition-aware relay workers for higher throughput
- Add deduplication keys and consumer-side idempotency store
- Replace polling with CDC (for example, Debezium-based streaming)
- Add retention/archival policy for sent outbox rows
- Add metrics for lag, retry count, and publish failure rate
