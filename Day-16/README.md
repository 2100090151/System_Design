# Day 16: Two-Phase Commit, Distributed Transactions, and Recovery

Day 16 focuses on how multiple services coordinate one logical transaction so they either all commit or all abort.

It covers:
- distributed transaction fundamentals
- two-phase commit (`prepare` and `commit/abort`)
- coordinator and participant roles
- commit durability and decision logging
- failure handling and uncertain participant states
- recovery and decision reconciliation

In short, Day 16 is about preserving cross-service consistency when one operation spans multiple systems.

![Day 16](./Day16.png)

[Day 16 PDF](./System_Design_Day_16.pdf)

## Core ideas

### Why distributed transactions matter
- A business action may touch multiple services (for example, payment, inventory, shipping).
- Independent local commits can leave partial state if one step fails.
- A distributed transaction protocol coordinates these services under one final decision.

### Two-phase commit phases
- Phase 1 (`prepare`): coordinator asks participants if they can commit.
- Participants reserve resources and respond `YES` or `NO`.
- Phase 2 (`decision`): if all voted `YES`, coordinator sends `COMMIT`; otherwise `ABORT`.

### Coordinator and participant responsibilities
- Coordinator drives the protocol, collects votes, and records final decision.
- Participants keep temporary/pending state during prepare.
- Participants finalize local state only after receiving the coordinator decision.

### Failure and recovery behavior
- If a participant misses the final decision (for example, temporary outage), it can remain uncertain.
- Coordinator must persist decision so it can be replayed later.
- Recovery path reconciles uncertain participants with the recorded decision.

### Trade-offs
- 2PC improves atomicity across services but adds latency and coordination overhead.
- Blocking can occur if coordinator fails before participants learn final decision.
- Many large systems prefer Saga patterns when high availability is prioritized over strict atomicity.

## Day-16 sample: Two-Phase Commit Coordinator

This repository includes a small 2PC sample in both Python and Java.

### Functional requirements
- Execute a transaction across multiple named participants
- Collect prepare votes before finalizing
- Commit only when all participants vote `YES`
- Abort when any participant votes `NO` or is unavailable
- Persist transaction decision for later reconciliation
- Recover a participant that missed the final decision

### High-level components
- `Participant`: handles prepare/finalize and keeps pending/committed state
- `Coordinator`: runs phase-1 and phase-2 logic
- `Decision log`: records `COMMIT` or `ABORT` per transaction
- `Reconcile path`: applies logged decisions to recovering participants

### Data flow
1. Client starts transaction with operations per participant
2. Coordinator sends prepare request to each participant
3. Participants vote and hold pending state if prepared
4. Coordinator logs final decision (`COMMIT` or `ABORT`)
5. Coordinator broadcasts decision to prepared participants
6. Recovering participant can request and apply logged decision later

## Project structure

```text
Day-16/
  README.md
  two-phase-commit-sample/
    python/
      two_phase_commit.py
      demo.py
    java/
      TwoPhaseCommitCoordinator.java
      Main.java
```

## Run the sample

### Python

```powershell
cd two-phase-commit-sample\python
python demo.py
```

### Java

```powershell
cd two-phase-commit-sample\java
javac Main.java TwoPhaseCommitCoordinator.java
java Main
```

## Interview takeaways
- 2PC gives all-or-nothing behavior across participants at the cost of coordination overhead
- Decision durability at coordinator is key for recovery correctness
- Participant uncertainty after failures is a core operational concern
- Blocking characteristics of 2PC are important when discussing availability trade-offs
- 2PC and Saga are complementary choices based on consistency requirements

## Next improvements
- Add coordinator crash and restart simulation with replayed decision log
- Add participant timeouts and retransmission behavior
- Add idempotent decision handling for repeated commit/abort messages
- Add Saga alternative flow for comparison in the same sample
- Add metrics for prepared duration and transaction completion latency
