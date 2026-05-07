# Day 32: Request Coalescing, Cache Stampede Protection, and One-Flight

Day 32 focuses on preventing cache-miss storms by collapsing concurrent identical requests into one backend fetch.

It covers:
- request coalescing fundamentals
- one-flight execution model
- cache stampede protection behavior
- waiter fan-in and shared completion
- cache TTL impact on backend load
- latency and capacity trade-offs

In short, Day 32 is about ensuring one backend call serves many concurrent misses for the same key instead of triggering duplicate work.

[Day 32 PDF](./System_Design_Day_32.pdf)

## Core ideas

### Why coalescing matters
- Hot keys can trigger many concurrent misses after TTL expiry.
- Without protection, all misses hit the backend at once.
- Coalescing keeps only one in-flight fetch per key and lets others wait.

### One-flight model
- First miss becomes the primary fetch owner.
- Later misses for the same key join as waiters.
- When fetch completes, all waiters receive the same value.

### Stampede protection flow
- Request checks cache first.
- On miss, request checks in-flight map for the key.
- If in-flight exists, join waiters; otherwise create new backend fetch.
- Completion fills cache and resolves all waiters.

### TTL and freshness
- Longer TTL lowers backend load but increases staleness window.
- Short TTL improves freshness but raises miss frequency.
- Coalescing reduces miss amplification even when TTL is short.

### Trade-offs
- Coalescing improves backend protection and often p95 under bursts.
- Waiters inherit primary fetch latency and may block longer than cache hits.
- Per-key lock map requires cleanup and bounded memory.

## Day-32 sample: Request Coalescing Simulator

This repository includes a request coalescing sample in both Python and Java.

### Functional requirements
- Accept requests arriving at specific timestamps for one hot key
- Apply cache TTL checks before backend fetch
- Allow only one in-flight backend fetch per key
- Attach concurrent misses as waiters to the same in-flight fetch
- Return request-level latencies and aggregate backend/load metrics

### High-level components
- `CacheEntry`: cached value and expiry time
- `InFlightFetch`: primary owner, waiters, and backend finish time
- `RequestCoalescer`: orchestration for cache checks and one-flight logic
- `SimulationResult`: request outcomes, timeline, and metrics

### Data flow
1. Request arrives and checks cache validity
2. Cache miss either starts new fetch or joins existing in-flight fetch
3. Backend completion writes cache entry and resolves all waiting requests
4. Later requests hit cache until TTL expires
5. Simulator reports hit ratios, coalesced requests, and backend calls saved

## Project structure

```text
Day-32/
  README.md
  request-coalescing-sample/
    python/
      request_coalescing.py
      demo.py
    java/
      RequestCoalescingSystem.java
      Main.java
```

## Run the sample

### Python

```powershell
cd request-coalescing-sample\python
python demo.py
```

### Java

```powershell
cd request-coalescing-sample\java
javac Main.java RequestCoalescingSystem.java
java Main
```

## Interview takeaways
- One-flight is a practical defense against cache stampedes
- Coalesced waiters can dramatically reduce duplicate backend load
- Cache TTL and coalescing should be tuned together
- Hot-key observability is required for safe rollout
- Coalescing complements stale-while-revalidate and rate limiting

## Next improvements
- Extend to multi-key simulation with per-key in-flight map
- Add stale-while-revalidate mode for instant stale responses
- Add jittered TTL expiry to reduce synchronized misses
- Add timeout/cancellation for waiters
- Add per-key memory guardrails for waiter lists
