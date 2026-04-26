# Day 21: Event Sourcing, Snapshotting, and Aggregate Rebuild

Day 21 focuses on storing domain changes as immutable events so current state can be reconstructed, audited, and replayed reliably.

It covers:
- event sourcing fundamentals
- append-only event store design
- aggregate rebuild from event history
- snapshotting for faster rehydration
- optimistic concurrency with expected versions
- replay and recovery trade-offs

In short, Day 21 is about making events the source of truth and deriving state from them safely.

![Day 21](./Day21.png)

[Day 21 PDF](./System_Design_Day_21.pdf)

## Core ideas

### Why Event Sourcing exists
- Traditional CRUD keeps only latest state and loses change history details.
- Event sourcing stores each business change as a durable domain event.
- State is derived by replaying events in order.

### Event store model
- Events are immutable and appended, never updated in place.
- Each aggregate stream has a version to preserve ordering.
- Writes commonly use optimistic concurrency (`expected_version`) to prevent lost updates.

### Aggregate rebuild
- Load all events for an aggregate and apply them in sequence.
- Business invariants are enforced in command handling before appending events.
- Rebuild logic should be deterministic and idempotent.

### Snapshotting
- Long event streams can make rebuild expensive.
- Snapshots persist aggregate state at a known version/sequence.
- Rehydrate by loading snapshot first, then replaying only tail events.

### Trade-offs
- Event sourcing improves auditability, replay, and temporal debugging.
- It adds complexity in schema evolution and projection maintenance.
- Strong event contracts and versioning strategy are essential.

## Day-21 sample: Event-Sourced Account Ledger

This repository includes a small event-sourcing sample in both Python and Java.

### Functional requirements
- Open an account with zero balance
- Deposit and withdraw funds with validation
- Persist events in append-only order
- Rebuild account state from events
- Create and use snapshots for faster restore

### High-level components
- `EventStore`: append-only event persistence with sequence numbers
- `SnapshotStore`: latest snapshot per account
- `AccountAggregate`: applies events to compute state
- `CommandService`: validates commands and appends events

### Data flow
1. Client issues command (`open_account`, `deposit`, `withdraw`)
2. Service rehydrates aggregate from snapshot + tail events
3. Service validates business rules
4. Service appends new domain event with expected version
5. Aggregate state can be rebuilt anytime via replay

## Project structure

```text
Day-21/
  README.md
  event-sourcing-sample/
    python/
      event_sourcing_account.py
      demo.py
    java/
      EventSourcingAccount.java
      Main.java
```

## Run the sample

### Python

```powershell
cd event-sourcing-sample\python
python demo.py
```

### Java

```powershell
cd event-sourcing-sample\java
javac Main.java EventSourcingAccount.java
java Main
```

## Interview takeaways
- Event sourcing makes historical behavior and auditing first-class
- Rebuild logic and event ordering correctness are critical
- Snapshots improve performance but must align with event sequence/version
- Optimistic concurrency protects aggregate consistency under parallel writes
- CQRS read models can be fed directly from event streams

## Next improvements
- Add event schema versioning and upcasters
- Add idempotency keys for command deduplication
- Add stream partitioning and retention strategy
- Add projection pipeline from events to query models
- Add monitoring for snapshot lag and replay latency
