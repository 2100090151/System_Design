  # Day 20: CQRS, Read-Model Projections, and Eventual Consistency

  Day 20 focuses on separating write-side command handling from read-side query models so systems can scale and evolve independently.

  It covers:
  - command-query responsibility segregation (`CQRS`) fundamentals
  - write model vs read model responsibilities
  - event log based projection pipeline
  - eventual consistency between command and query paths
  - replay and read-model rebuild behavior
  - design trade-offs for complexity vs scalability

  In short, Day 20 is about optimizing writes and reads separately while handling asynchronous projection lag safely.

  ![Day 20](./Day20.png)

  [Day 20 PDF](./System_Design_Day_20.pdf)

  ## Core ideas

  ### Why CQRS exists
  - Write workflows and read workflows usually have different performance and schema needs.
  - A single model optimized for both often becomes slow and hard to maintain.
  - CQRS separates command handling from query read paths.

  ### Command side
  - Command APIs validate intent and enforce business rules.
  - Successful commands mutate the write model and emit events.
  - Command responses focus on acknowledgement, not rich query payloads.

  ### Query side
  - Read model is built for query patterns, not transaction invariants.
  - Projectors consume events and update denormalized views.
  - Query APIs stay fast because they avoid write-side logic.

  ### Eventual consistency
  - Read model can lag behind recent writes.
  - Clients may temporarily observe stale data until projection catches up.
  - UIs and APIs should communicate processing state where needed.

  ### Trade-offs
  - CQRS improves scalability and clarity for complex domains.
  - It increases architecture complexity (projectors, offsets, replay, monitoring).
  - Idempotent projection and replay-safe handlers are critical for correctness.

  ## Day-20 sample: CQRS Order Projection

  This repository includes a small CQRS sample in both Python and Java.

  ### Functional requirements
  - Accept commands to create and confirm orders
  - Store command-side aggregate state
  - Emit domain events to an append-only event log
  - Build a query-side read model using a projector
  - Demonstrate lag between write and read paths

  ### High-level components
  - `CommandService`: validates and applies write-side commands
  - `WriteStore`: aggregate state and event log
  - `Projector`: applies events to read model with tracked offset
  - `ReadStore` and `QueryService`: query-side optimized access

  ### Data flow
  1. Client sends command (`create_order`, `confirm_order`)
  2. Command side validates and writes aggregate state
  3. Command side appends event to event log
  4. Projector polls new events from last offset
  5. Read model is updated from projected events
  6. Query API serves denormalized read model

  ## Project structure

  ```text
  Day-20/
    README.md
    cqrs-sample/
      python/
        cqrs_order_system.py
        demo.py
      java/
        CqrsOrderSystem.java
        Main.java
  ```

  ## Run the sample

  ### Python

  ```powershell
  cd cqrs-sample\python
  python demo.py
  ```

  ### Java

  ```powershell
  cd cqrs-sample\java
  javac Main.java CqrsOrderSystem.java
  java Main
  ```

  ## Interview takeaways
  - CQRS is useful when read/write workloads and models diverge significantly
  - Event projection lag is normal and should be explicit in design discussion
  - Replay capability is essential for read-model recovery and evolution
  - Idempotent projectors prevent corruption during retries or reprocessing
  - CQRS can pair effectively with Outbox, Saga, and event-driven systems

  ## Next improvements
  - Add projection retries and dead-letter handling
  - Add idempotency keys for command deduplication
  - Add snapshotting for faster aggregate rebuilds
  - Add multiple read models for different query shapes
  - Add metrics for projection lag and event throughput
