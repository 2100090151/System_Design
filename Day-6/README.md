# Day 6: Coordination, Idempotency, and Distributed Workflows

Day 6 focuses on how distributed systems stay correct when requests are retried, events are duplicated, and multi-service workflows fail halfway through.

It covers:
- timeouts, retries, exponential backoff, and jitter
- idempotency and deduplication
- at-most-once, at-least-once, and the "exactly-once" trade-off
- distributed locks, leases, and leader election
- sagas and compensating transactions
- outbox and inbox patterns
- workflow orchestration vs choreography

In short, Day 6 is about protecting business correctness when a system is no longer a single transaction on a single machine.

![Day 6](./Day6.png)

## Core ideas

### Timeouts, retries, and backoff
- `Timeouts` stop resources from being blocked forever by slow dependencies.
- `Retries` improve success rate for transient failures, but they can amplify load if done carelessly.
- `Exponential backoff` and `jitter` reduce retry storms and make recovery smoother under failure.

### Idempotency and deduplication
- `Idempotency` means repeating the same request does not create a different business outcome.
- This matters for payments, order creation, webhook delivery, and message consumption.
- A common implementation stores the result of an `idempotency key` and returns the saved response on duplicate requests.

### Delivery guarantees
- `At-most-once` avoids duplicates, but can lose work.
- `At-least-once` avoids loss, but consumers must handle duplicates.
- `Exactly-once` is usually only achievable within narrow boundaries and still depends on idempotent application logic.

### Locks, leases, and leader election
- `Distributed locks` coordinate access to shared resources across instances.
- `Leases` are time-bound locks that reduce permanent deadlock risk when an owner crashes.
- `Leader election` picks one active coordinator for tasks like scheduling, partition ownership, or failover management.

### Sagas, outbox, and workflow control
- A `saga` breaks one large distributed transaction into smaller local transactions with compensating actions.
- The `outbox pattern` writes business state and pending events together so events are not lost between database and broker.
- `Orchestration` centralizes workflow control, while `choreography` lets services react to events independently.

## Day-6 sample: Idempotent Request Processor

This repository includes a small idempotency-key sample in both Python and Java.

### Functional requirements
- Accept a request with an idempotency key
- Process a new request exactly once at the application level
- Return the stored response when the same key is retried
- Keep the implementation simple enough for interview explanation

### High-level components
- `Client`: sends a request and retry key
- `API/service handler`: checks whether the key was already seen
- `Idempotency store`: keeps the response for processed keys
- `Business logic`: performs the actual action only for new keys

### Data flow
1. A client sends a request with an idempotency key such as `payment-req-101`
2. The service checks whether that key already exists in the idempotency store
3. If the key is new, the business action runs and its response is stored
4. If the same request is retried, the stored response is returned
5. The client sees a stable result even if the network caused duplicate submissions

## Project structure

```text
Day-6/
  README.md
  idempotency-sample/
    python/
      idempotency_store.py
      demo.py
    java/
      IdempotencyStore.java
      Main.java
```

## Run the sample

### Python

```powershell
cd idempotency-sample\python
python demo.py
```

### Java

```powershell
cd idempotency-sample\java
javac Main.java IdempotencyStore.java
java Main
```

## Interview takeaways
- Retries are only safe when the write path is idempotent
- "Exactly-once" usually turns into at-least-once delivery plus deduplication or idempotent consumers
- Distributed locks solve coordination problems, but they also introduce failure and expiry edge cases
- Sagas are often more practical than distributed two-phase commit in service-oriented architectures
- Outbox and inbox patterns are common when events must stay consistent with database state

## Next improvements
- Track `in-progress` requests separately from completed ones
- Add response status codes and error caching rules
- Add request payload hashing to reject key reuse with different payloads
- Persist keys in Redis or a database with TTL support
- Extend the sample into a payment or order creation API
