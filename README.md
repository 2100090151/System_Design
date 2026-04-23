# System Design Preparation

This repository tracks day-wise system design preparation notes and sample projects.

## Contents

- [Day-1](./Day-1/README.md): Introduction to system design and a URL shortener sample in Python and Java.
- [Day-2](./Day-2/README.md): Networking, scalability, and delivery fundamentals with a round-robin load balancer sample.
- [Day-3](./Day-3/README.md): Databases, consistency, and partitioning fundamentals with a consistent hashing sample.
- [Day-4](./Day-4/README.md): Architecture, messaging, and API communication fundamentals with a publish-subscribe event bus sample.
- [Day-5](./Day-5/README.md): Reliability, security, and platform operations fundamentals with a token bucket rate limiter sample.
- [Day-6](./Day-6/README.md): Coordination, idempotency, and distributed workflow fundamentals with an idempotent request processor sample.
- [Day-7](./Day-7/README.md): Observability, monitoring, and tracing fundamentals with a trace context propagator sample.
- [Day-8](./Day-8/README.md): Search, indexing, and information retrieval fundamentals with an inverted index search sample.
- [Day-9](./Day-9/README.md): Caching, eviction, and data access performance fundamentals with an LRU cache sample.
- [Day-10](./Day-10/README.md): Queues, retries, and dead-letter handling fundamentals with a retry queue sample.
- [Day-11](./Day-11/README.md): Streams, partitioned logs, and consumer-group offset management fundamentals with a partitioned log sample.
- [Day-12](./Day-12/README.md): Probabilistic data structures and Bloom filter fundamentals with a Bloom filter sample.
- [Day-13](./Day-13/README.md): Merkle tree, data integrity, and anti-entropy fundamentals with a Merkle tree sample.
- [Day-14](./Day-14/README.md): Quorum replication, tunable consistency, and read-repair fundamentals with a quorum key-value sample.
- [Day-15](./Day-15/README.md): Raft consensus, leader election, and log replication fundamentals with a simplified Raft sample.
- [Day-16](./Day-16/README.md): Two-phase commit, distributed transactions, and recovery fundamentals with a 2PC coordinator sample.
- [Day-17](./Day-17/README.md): SOLID principles and maintainable service design fundamentals with a SOLID alert routing sample.
- [Day-18](./Day-18/README.md): Saga pattern, compensating transactions, and orchestration fundamentals with an order saga sample.

## Goal 

Build strong fundamentals in:
- requirement analysis
- high-level and low-level design
- scalability and reliability
- data modeling and trade-offs

## Day 1

Introduction to system design and why architecture decisions matter early.

- Topic: what system design is
- Focus: requirements, architecture, interfaces, and data design
- Sample project: URL shortener in Python and Java

[Read Day-1 notes](./Day-1/README.md)

![Day 1](./Day-1/Day1.png)

## Day 2

Networking, traffic flow, scalability, and delivery fundamentals for distributed systems.

- Topics: IP, OSI, TCP/UDP, DNS, load balancing, clustering, caching, CDN, proxy
- Focus: how requests move through systems and how to keep them available and fast
- Sample project: round-robin load balancer in Python and Java

[Read Day-2 notes](./Day-2/README.md)

![Day 2](./Day-2/Day2.png)

## Day 3

Databases, consistency, partitioning, and distributed data fundamentals.

- Topics: SQL vs NoSQL, replication, indexes, normalization, ACID/BASE, CAP, sharding, consistent hashing
- Focus: how large systems store and distribute data safely at scale
- Sample project: consistent hashing shard router in Python and Java

[Read Day-3 notes](./Day-3/README.md)

![Day 3](./Day-3/Day3.png)

## Day 4

Architectural patterns, asynchronous communication, API choices, and real-time delivery models.

- Topics: N-tier architecture, queues, pub/sub, ESB, monoliths vs microservices, EDA, CQRS, API Gateway, REST/GraphQL/gRPC, WebSockets
- Focus: how services are structured and how they communicate without tight coupling
- Sample project: publish-subscribe event bus in Python and Java

[Read Day-4 notes](./Day-4/README.md)

![Day 4](./Day-4/Day4.png)

## Day 5

Reliability, security, service discovery, and platform operations fundamentals for production distributed systems.

- Topics: geohashing, quadtrees, circuit breaker, rate limiting, service discovery, SLA/SLO/SLI, disaster recovery, VMs, containers, OAuth, OIDC, SSO, TLS
- Focus: how to keep systems resilient, discoverable, and secure after deployment
- Sample project: token bucket rate limiter in Python and Java

[Read Day-5 notes](./Day-5/README.md)

![Day 5](./Day-5/Day5.png)

## Day 6

Coordination, retries, and workflow correctness fundamentals for distributed systems.

- Topics: timeouts, retries, exponential backoff, jitter, idempotency, delivery guarantees, distributed locks, leader election, sagas, outbox/inbox, orchestration vs choreography
- Focus: how to keep business operations correct when requests are duplicated or workflows fail midway
- Sample project: idempotent request processor in Python and Java

[Read Day-6 notes](./Day-6/README.md)

![Day 6](./Day-6/Day6.png)

## Day 7

Observability, tracing, and production debugging fundamentals for distributed systems.

- Topics: logs, metrics, traces, structured logging, correlation IDs, RED/USE metrics, dashboards, alerting, distributed tracing, sampling, telemetry cost
- Focus: how to detect problems quickly and reconstruct request behavior across services
- Sample project: trace context propagator in Python and Java

[Read Day-7 notes](./Day-7/README.md)

![Day 7](./Day-7/Day7.png)

## Day 8

Search, indexing, and information retrieval fundamentals for distributed systems.

- Topics: search basics, inverted indexes, tokenization, ranking, autocomplete, indexing pipelines, sharding, replication, query fan-out
- Focus: how to organize large content collections so users can retrieve relevant results quickly
- Sample project: inverted index search in Python and Java

[Read Day-8 notes](./Day-8/README.md)

![Day 8](./Day-8/Day8.png)


## Day 9

Caching, eviction, and data access performance fundamentals for distributed systems.

- Topics: cache patterns, TTL, invalidation, eviction policies, local vs distributed cache, hot keys, stampedes, consistency
- Focus: how to reduce read latency and backend load without losing freshness control
- Sample project: LRU cache in Python and Java

[Read Day-9 notes](./Day-9/README.md)

![Day 9](./Day-9/Day9.png)

## Day 10

Queues, retries, and dead-letter handling fundamentals for asynchronous processing workflows.

- Topics: asynchronous queues, at-least-once delivery, idempotency, retries, dead-letter queues, backpressure
- Focus: how to keep background processing reliable when workers fail or downstream systems are slow
- Sample project: retry queue with dead-letter handling in Python and Java

[Read Day-10 notes](./Day-10/README.md)

![Day 10](./Day-10/Day10.png)

## Day 11

Streams, partitioned logs, and consumer-group offset management fundamentals for event-driven systems.

- Topics: append-only logs, partitioning, ordering guarantees, consumer groups, offsets, replay, retention, lag
- Focus: how to decouple producers and consumers while preserving scalable throughput and recoverable consumption
- Sample project: partitioned log with consumer groups and explicit offset commits in Python and Java

[Read Day-11 notes](./Day-11/README.md)

![Day 11](./Day-11/Day11.png)

## Day 12

Probabilistic data structures and memory-efficient membership testing fundamentals for large-scale systems.

- Topics: Bloom filters, false positives, approximate membership, memory vs accuracy trade-offs, cache penetration protection, sketch-based thinking
- Focus: how to answer membership-style questions cheaply before falling back to an exact system of record
- Sample project: Bloom filter in Python and Java

[Read Day-12 notes](./Day-12/README.md)

![Day 12](./Day-12/Day12.png)

## Day 13

Merkle trees, data integrity, and anti-entropy fundamentals for distributed storage and synchronization.

- Topics: Merkle trees, root hashes, inclusion proofs, replica comparison, anti-entropy, hashing trade-offs
- Focus: how to compare and verify large datasets efficiently without transferring every record
- Sample project: Merkle tree in Python and Java

[Read Day-13 notes](./Day-13/README.md)

![Day 13](./Day-13/Day13.png)

## Day 14

Quorum replication, tunable consistency, and read-repair fundamentals for replicated databases.

- Topics: quorum math (`N`, `R`, `W`), tunable consistency, stale reads, read repair, replication trade-offs
- Focus: how to balance consistency, latency, and availability when replicas diverge
- Sample project: quorum key-value store with read repair in Python and Java

[Read Day-14 notes](./Day-14/README.md)

![Day 14](./Day-14/Day14.png)

## Day 15

Raft consensus, leader election, and replicated log fundamentals for distributed coordination.

- Topics: Raft roles and terms, majority voting, leader election, log replication, commit rules, safety trade-offs
- Focus: how a cluster agrees on one ordered history of updates even when some nodes fail
- Sample project: simplified Raft consensus simulation in Python and Java

[Read Day-15 notes](./Day-15/README.md)

![Day 15](./Day-15/Day15.png)

## Day 16

Two-phase commit, distributed transactions, and failure-recovery fundamentals for multi-service consistency.

- Topics: transaction coordinator/participant roles, prepare/commit phases, decision logs, uncertain states, reconciliation
- Focus: how to keep cross-service operations atomic when one business action spans multiple systems
- Sample project: two-phase commit coordinator simulation in Python and Java

[Read Day-16 notes](./Day-16/README.md)

![Day 16](./Day-16/Day16.png)

## Day 17

SOLID principles and maintainable service design fundamentals for extensible, testable software architecture.

- Topics: SRP, OCP, LSP, ISP, DIP, interface design, dependency management
- Focus: how to design modules and services that are easier to extend safely with low coupling
- Sample project: SOLID alert routing service in Python and Java

[Read Day-17 notes](./Day-17/README.md)

![Day 17](./Day-17/Day17.png)

[Day-17 PDF](./Day-17/System_Design_Day_17.pdf)

## Day 18

Saga pattern, compensating transactions, and orchestration fundamentals for distributed workflows.

- Topics: local transactions, orchestration, compensation logic, rollback sequencing, eventual consistency
- Focus: how to keep multi-service business flows correct without strict global transactions
- Sample project: order saga orchestrator in Python and Java

[Read Day-18 notes](./Day-18/README.md)

![Day 18](./Day-18/Day18.png)

[Day-18 PDF](./Day-18/System_Design_Day_18.pdf)
