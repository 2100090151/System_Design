# Day 11: Streams, Partitioned Logs, and Consumer Groups

Day 11 focuses on how event-driven systems move high-throughput data safely from producers to many independent consumers.

It covers:
- append-only event logs
- partitioning for scale
- ordering guarantees and key-based routing
- consumer groups and offset tracking
- replay and recovery
- lag monitoring and backpressure

In short, Day 11 is about building reliable stream processing foundations where data can be consumed, retried, and replayed without tight coupling.

![Day 11](./Day11.png)

[Day 11 PDF](./System_Design_Day_11.pdf)

## Core ideas

### Append-only logs
- A `stream log` stores events in order and never rewrites historical records.
- Producers append events, while consumers read by offset.
- This model makes replay and auditing straightforward.

### Partitioning and ordering
- A topic is split into `partitions` so writes and reads can scale horizontally.
- Ordering is guaranteed within one partition, not across all partitions.
- Key-based partitioning keeps related events together (for example, all events for one user).

### Consumer groups and offsets
- Each consumer group tracks its own committed offset per partition.
- Different groups can process the same stream independently.
- If a group crashes, it can resume from committed offsets instead of starting over.

### Replay, lag, and backpressure
- Replaying from older offsets helps recovery, reprocessing, and debugging.
- `Consumer lag` shows how far a group is behind the latest offset.
- Growing lag signals that consumers are slow or under-provisioned.

## Day-11 sample: Partitioned Log with Consumer Offsets

This repository includes a small partitioned log sample in both Python and Java.

### Functional requirements
- Produce events with key-based partition routing
- Register multiple consumer groups
- Poll events without auto-committing offsets
- Commit processed offsets explicitly
- Compute per-partition lag for each consumer group

### High-level components
- `Producer`: appends keyed events
- `PartitionedLog`: stores events by partition and offset
- `Consumer group state`: tracks committed offsets per partition
- `Poll logic`: reads events from committed offsets
- `Commit logic`: advances offsets after successful processing

### Data flow
1. Producer appends events into a partitioned log
2. A consumer group polls a batch from its current committed offsets
3. Successfully processed events are committed
4. Uncommitted events are replayed on the next poll
5. Lag reflects how far each group is behind

## Project structure

```text
Day-11/
  README.md
  partitioned-log-sample/
    python/
      partitioned_log.py
      demo.py
    java/
      PartitionedLog.java
      Main.java
```

## Run the sample

### Python

```powershell
cd partitioned-log-sample\python
python demo.py
```

### Java

```powershell
cd partitioned-log-sample\java
javac Main.java PartitionedLog.java
java Main
```

## Interview takeaways
- Partitioning increases throughput but limits ordering guarantees to each partition
- Consumer groups isolate processing concerns while sharing the same source stream
- Explicit offset commits are central to recovery and duplicate handling
- Replay is a feature, not an accident, in stream systems
- Lag is one of the most important operational indicators in event-driven architectures

## Next improvements
- Add retention windows and offset expiration
- Add consumer rebalancing simulation across multiple members
- Add idempotency keys for downstream consumers
- Add delayed retries and poison-event routing
- Replace in-memory log with Kafka, Pulsar, or Kinesis
