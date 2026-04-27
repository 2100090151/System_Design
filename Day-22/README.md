# Day 22: Event Versioning, Schema Evolution, and Upcasting

Day 22 focuses on how event-driven systems evolve event schemas safely without breaking replay, projections, or consumer compatibility.

It covers:
- event schema versioning fundamentals
- backward and forward compatibility concerns
- upcaster pattern for legacy event migration
- replay safety with mixed-version event streams
- defaulting and transformation strategies
- operational trade-offs in long-lived event stores

In short, Day 22 is about keeping old and new events usable together as systems evolve.

![Day 22](./Day22.png)

[Day 22 PDF](./System_Design_Day_22.pdf)

## Core ideas

### Why version events
- Event contracts change as product requirements evolve.
- Historical events remain in storage and must still be replayable.
- Versioning prevents silent corruption when payload shapes diverge.

### Compatibility concerns
- Older consumers may not understand new fields or formats.
- New consumers must still process legacy events in existing streams.
- Clear compatibility policy is required per event type.

### Upcasting pattern
- `Upcasting` transforms older event versions into a canonical latest shape at read time.
- Instead of rewriting the full event store, transforms are applied during replay.
- This keeps storage immutable while enabling code evolution.

### Replay and projection safety
- Projectors should operate on canonical event shapes.
- Missing upcasters or inconsistent defaults can break deterministic rebuilds.
- Replay tests are critical after schema changes.

### Trade-offs
- Upcasting reduces migration risk but adds runtime transformation cost.
- Version chains can grow and become harder to reason about.
- Periodic compaction or snapshotting can reduce long-term replay overhead.

## Day-22 sample: Event Upcasting Ledger

This repository includes a small event-versioning sample in both Python and Java.

### Functional requirements
- Store events with explicit schema versions
- Keep legacy (`v1`) and latest (`v2`) events in the same stream
- Upcast old events into canonical latest form during read/replay
- Rebuild account balance from mixed-version history
- Fail fast for unsupported version transitions

### High-level components
- `EventStore`: append-only event storage
- `EventUpcaster`: transforms old versions to latest
- `AccountProjector`: applies canonical events to rebuild state
- `CommandService`: emits latest-version events

### Data flow
1. Legacy and current services append events with their own schema versions
2. Rebuild reads raw events from the stream
3. Upcaster converts each event to latest supported version
4. Projector applies canonical events in sequence
5. Final aggregate state is returned from replay

## Project structure

```text
Day-22/
  README.md
  event-upcasting-sample/
    python/
      event_upcasting_ledger.py
      demo.py
    java/
      EventUpcastingLedger.java
      Main.java
```

## Run the sample

### Python

```powershell
cd event-upcasting-sample\python
python demo.py
```

### Java

```powershell
cd event-upcasting-sample\java
javac Main.java EventUpcastingLedger.java
java Main
```

## Interview takeaways
- Event evolution strategy should be explicit from day one in event-sourced systems
- Upcasting is a common way to preserve immutable history with evolving schemas
- Canonical event handling simplifies projector correctness and testing
- Replay validation is a mandatory guardrail before deploying schema changes
- Version debt accumulates, so pruning/compaction strategy matters long term

## Next improvements
- Add multi-step upcasting chains (v1 -> v2 -> v3)
- Add schema registry compatibility checks for producers
- Add snapshot-based rebuild acceleration after heavy upcasting
- Add projection contract tests with golden historical fixtures
- Add metrics for upcast hit rate and replay latency
