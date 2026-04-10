# System Design Preparation

This repository tracks day-wise system design preparation notes and sample projects.

## Contents

- [Day-1](./Day-1/README.md): Introduction to system design and a URL shortener sample in Python and Java.
- [Day-2](./Day-2/README.md): Networking, scalability, and delivery fundamentals with a round-robin load balancer sample.
- [Day-3](./Day-3/README.md): Databases, consistency, and partitioning fundamentals with a consistent hashing sample.
- [Day-4](./Day-4/README.md): Architecture, messaging, and API communication fundamentals with a publish-subscribe event bus sample.
- [Day-5](./Day-5/README.md): Reliability, security, and platform operations fundamentals with a token bucket rate limiter sample.

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
