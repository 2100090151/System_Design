# Day 17: SOLID Principles and Maintainable Service Design

Day 17 focuses on object-oriented design choices that keep services extensible, testable, and easier to change safely.

It covers:
- Single Responsibility Principle (`SRP`)
- Open/Closed Principle (`OCP`)
- Liskov Substitution Principle (`LSP`)
- Interface Segregation Principle (`ISP`)
- Dependency Inversion Principle (`DIP`)
- applying SOLID to production service boundaries and maintainability

In short, Day 17 is about reducing coupling so systems evolve with fewer regressions and less rewrite pressure.

![Day 17](./Day17.png)

[Day 17 PDF](./System_Design_Day_17.pdf)

## Core ideas

### Single Responsibility Principle (SRP)
- A class/module should have one clear reason to change.
- Mixing persistence, business logic, and IO in one unit increases breakage risk.
- Separate concerns make behavior easier to test and reason about.

### Open/Closed Principle (OCP)
- Software units should be open for extension but closed for modification.
- Add behavior through new implementations rather than rewriting stable orchestration code.
- This reduces regression surface when requirements change.

### Liskov Substitution Principle (LSP)
- Subtypes must be safely replaceable for their base contracts.
- If one implementation violates assumptions, shared orchestration logic becomes brittle.
- Strong contracts plus consistent behavior keep polymorphism safe.

### Interface Segregation Principle (ISP)
- Clients should not depend on methods they do not need.
- Prefer narrow interfaces (read-only, write-only, etc.) over one large "god" interface.
- Smaller contracts improve modularity and limit accidental coupling.

### Dependency Inversion Principle (DIP)
- High-level policy should depend on abstractions, not concrete low-level details.
- Constructor injection and interface-based wiring improve testability.
- This enables replacing infrastructure (storage, channels) without rewriting business flow.

## Day-17 sample: SOLID Alert Routing Service

This repository includes a small SOLID sample in both Python and Java.

### Functional requirements
- Create alerts with a severity and message
- Persist incoming alerts
- Route alerts to matching delivery channels
- Add new channels without changing dispatcher logic
- Keep read and write repository contracts separate
- Audit critical alert counts from read-only access

### High-level components
- `Alert`: immutable domain object
- `AlertWriter` and `AlertReader`: segregated contracts
- `InMemoryAlertRepository`: concrete storage implementation
- `AlertChannel`: pluggable channel abstraction
- `AlertDispatcher`: high-level routing policy
- `AlertAuditService`: read-only audit operation

### Data flow
1. A producer creates an alert
2. Dispatcher stores it through `AlertWriter`
3. Dispatcher iterates `AlertChannel` implementations
4. Each channel decides if it supports the alert severity
5. Supported channels send and record delivery
6. Audit service reads stored alerts through `AlertReader`

## Project structure

```text
Day-17/
  README.md
  solid-alert-sample/
    python/
      solid_alert_system.py
      demo.py
    java/
      SolidAlertSystem.java
      Main.java
```

## Run the sample

### Python

```powershell
cd solid-alert-sample\python
python demo.py
```

### Java

```powershell
cd solid-alert-sample\java
javac Main.java SolidAlertSystem.java
java Main
```

## Interview takeaways
- SOLID is mainly about change resilience, not code style aesthetics
- OCP + DIP together reduce risk when integrating new capabilities
- ISP keeps service contracts clean as systems scale
- LSP violations are a common hidden source of runtime bugs
- SRP improves observability, testing speed, and team ownership boundaries

## Next improvements
- Add retry and dead-letter handling per delivery channel
- Replace in-memory repository with a persistent store
- Add per-channel rate limiting and backoff policies
- Add metrics for routed alerts by severity/channel
- Add contract tests to validate channel substitutability (LSP)
