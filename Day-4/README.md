# Day 4: Architecture, Messaging, and APIs

Day 4 focuses on how services are organized and how they communicate inside distributed systems.

It covers:
- N-tier architecture and separation of responsibilities
- Message brokers, message queues, and publish-subscribe
- Enterprise Service Bus (ESB)
- Monoliths, modular monoliths, and microservices
- Event-Driven Architecture (EDA), event sourcing, and CQRS
- API Gateway and Backend For Frontend (BFF)
- REST, GraphQL, and gRPC
- Long polling, WebSockets, and Server-Sent Events (SSE)

In short, Day 4 is about choosing the right communication model and service boundaries so the system remains scalable, maintainable, and responsive.

![Day 4](./Day4.png)

## Core ideas

### N-tier architecture
- `N-tier architecture` separates presentation, business logic, and data access concerns.
- It improves maintainability and scalability by isolating responsibilities.
- More tiers usually improve separation, but they also add network hops and operational complexity.

### Message brokers, queues, and publish-subscribe
- `Message brokers` help services communicate asynchronously without direct coupling.
- `Message queues` are point-to-point and are good for background jobs and work distribution.
- `Publish-subscribe` broadcasts events to many consumers and is useful for fan-out notifications and decoupled event handling.

### ESB, monoliths, and microservices
- `ESB` centralizes integration logic, but it can become a bottleneck and a single point of failure.
- `Monoliths` are simple to start with and easier to debug early on.
- `Microservices` trade simplicity for team autonomy, independent scaling, and better fault isolation.
- A poorly designed microservices system can become a distributed monolith.

### Event-Driven Architecture, event sourcing, and CQRS
- `EDA` uses events to communicate state changes between services.
- `Event sourcing` stores domain changes as an append-only log instead of only storing the latest state.
- `CQRS` separates write paths from read paths so each can scale and evolve differently.

### API Gateway and API styles
- `API Gateway` provides a single entry point for clients and can handle authentication, routing, rate limiting, logging, and caching.
- `REST` is simple and widely supported.
- `GraphQL` reduces over-fetching and gives the client more control over response shape.
- `gRPC` is efficient for service-to-service communication and supports code generation and streaming.

### Real-time communication
- `Long polling` works but is inefficient at scale.
- `WebSockets` provide full-duplex, persistent communication and are strong for chat and live updates.
- `SSE` is server-to-client only and is simpler than WebSockets for streaming updates.

## Day-4 sample: Publish-Subscribe Event Bus

This repository includes a small publish-subscribe event bus sample in both Python and Java.

### Functional requirements
- Allow services to subscribe to a topic
- Publish events to a topic
- Deliver every event to all subscribers of that topic
- Keep the API simple enough to explain in an interview

### High-level components
- `Publisher`: emits an event
- `Event bus`: stores subscriptions and dispatches messages
- `Topic`: logical grouping for events
- `Subscribers`: services that react to the event independently

### Data flow
1. Services subscribe handlers to a topic such as `order.created`
2. A publisher emits an event to that topic
3. The event bus fans the event out to all subscribed handlers
4. Each subscriber processes the event independently
5. New subscribers can be added without changing the publisher

## Project structure

```text
Day-4/
  README.md
  pub-sub-sample/
    python/
      event_bus.py
      demo.py
    java/
      EventBus.java
      Main.java
```

## Run the sample

### Python

```powershell
cd pub-sub-sample\python
python demo.py
```

### Java

```powershell
cd pub-sub-sample\java
javac Main.java EventBus.java
java Main
```

## Interview takeaways
- Service boundaries should follow the business domain, not just technical layers
- Async messaging improves decoupling, but delivery guarantees and ordering become important
- API Gateway is useful, but it must not turn into a bottleneck
- REST, GraphQL, and gRPC solve different problems; pick based on client needs and latency profile
- WebSockets are usually the right choice when two-way, low-latency communication matters

## Next improvements
- Add unsubscribe support
- Add retry and dead-letter queue simulation
- Add event persistence for replay
- Add message filtering and wildcard topics
- Extend the sample into an event-driven order processing workflow
