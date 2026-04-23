# Day 18: Saga Pattern, Compensating Transactions, and Orchestration

Day 18 focuses on how distributed systems keep business workflows consistent without using strict global transactions.

It covers:
- saga pattern fundamentals
- orchestration-based workflow control
- local transactions per service
- compensating actions for rollback behavior
- failure handling across multi-step flows
- eventual consistency trade-offs

In short, Day 18 is about coordinating long-running distributed operations with explicit forward and rollback steps.

![Day 18](./Day18.png)

[Day 18 PDF](./System_Design_Day_18.pdf)

## Core ideas

### Why Saga exists
- Cross-service workflows are hard to run under one ACID transaction.
- `Saga` splits a business flow into multiple local transactions.
- If one step fails, previously completed steps are compensated in reverse order.

### Orchestration model
- A central orchestrator decides which step runs next.
- Each service exposes `execute` and compensation behavior.
- This model gives clear visibility and deterministic rollback order.

### Compensation behavior
- Compensation is not a DB rollback; it is a semantic undo action.
- For example: reserve inventory -> later release inventory.
- Compensations must be idempotent because retries are common in distributed systems.

### Failure handling
- A failed step marks the saga as failed.
- Orchestrator triggers compensations for already completed steps.
- If a compensation itself fails, the workflow enters a recovery/alert state and needs retries or manual intervention.

### Trade-offs
- Saga improves availability and autonomy of services compared to 2PC.
- Consistency is eventual, not immediate across all services.
- Correctness depends heavily on well-designed compensating actions.

## Day-18 sample: Order Saga Orchestrator

This repository includes a small Saga orchestration sample in both Python and Java.

### Functional requirements
- Execute an order workflow across inventory, payment, and shipping steps
- Stop workflow when a step fails
- Trigger compensations for previously completed steps
- Record step-by-step execution history
- Surface final saga state (`COMPLETED` or `FAILED`)

### High-level components
- `SagaStep`: interface/contract for execute and compensate
- `SagaOrchestrator`: coordinates step execution and rollback sequence
- Domain steps: inventory reserve/release, payment charge/refund, shipping create/cancel
- `SagaResult`: final outcome and execution log

### Data flow
1. Orchestrator starts with ordered steps
2. Each step executes local business action
3. On success, step is tracked as completed
4. On failure, orchestrator compensates completed steps in reverse order
5. Saga result returns status and timeline for observability/debugging

## Project structure

```text
Day-18/
  README.md
  order-saga-sample/
    python/
      order_saga.py
      demo.py
    java/
      OrderSaga.java
      Main.java
```

## Run the sample

### Python

```powershell
cd order-saga-sample\python
python demo.py
```

### Java

```powershell
cd order-saga-sample\java
javac Main.java OrderSaga.java
java Main
```

## Interview takeaways
- Saga is a practical alternative to 2PC for high-availability microservices
- Compensation design is part of business modeling, not just engineering plumbing
- Orchestration gives stronger control/traceability than pure choreography for critical flows
- Idempotency is mandatory for both forward and compensating actions
- Eventual consistency should be explicit in user and API expectations

## Next improvements
- Add retry/backoff policies for both execute and compensate paths
- Persist saga state so execution survives orchestrator restarts
- Add timeout handling and step-level circuit breakers
- Add dead-letter handling for failed compensations
- Add metrics and tracing IDs for each saga instance
