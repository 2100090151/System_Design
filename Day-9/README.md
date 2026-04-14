# Day 9: Caching, Eviction, and Data Access Performance

Day 9 focuses on how systems reduce repeated work and lower latency by storing hot data closer to the read path.

It covers:
- cache fundamentals
- cache-aside, read-through, and write-through patterns
- TTL, refresh, and invalidation
- LRU, LFU, and FIFO eviction policies
- cache consistency and stale reads
- local cache vs distributed cache
- hot keys and cache stampede protection

In short, Day 9 is about improving read performance without losing control of correctness, freshness, or operational cost.

![Day 9](./Day9.png)

## Core ideas

### Cache fundamentals
- A `cache` stores frequently accessed data so the system does not have to fetch or compute it every time.
- Caches reduce latency and lower load on databases or downstream services.
- A cache only helps when the hit rate is high enough to justify its complexity.

### Read and write patterns
- `Cache-aside` lets the application load data into the cache on demand.
- `Read-through` hides cache misses behind the cache layer itself.
- `Write-through` updates the cache immediately with writes, while `write-back` delays persistence and is riskier.

### TTL, invalidation, and freshness
- `TTL` limits how long cached data can stay before it expires.
- `Invalidation` is hard because stale data can survive longer than the business expects.
- Many systems accept bounded staleness to gain performance and availability.

### Eviction policies
- `LRU` evicts the least recently used item.
- `LFU` evicts the least frequently used item.
- `FIFO` evicts the oldest inserted item regardless of access pattern.
- The right policy depends on whether recency, frequency, or simplicity matters more.

### Distributed caching risks
- `Distributed caches` improve sharing across instances, but they add network latency and failure modes.
- Hot keys can overload one shard or one cached item.
- Cache stampedes happen when many requests miss together and all hit the database at once.

## Day-9 sample: LRU Cache

This repository includes a small LRU cache sample in both Python and Java.

### Functional requirements
- Put key-value pairs into the cache
- Get values by key
- Move recently accessed items to the front of usage order
- Evict the least recently used item when capacity is exceeded

### High-level components
- `Client`: reads and writes cached keys
- `Cache map`: stores key-to-node lookup
- `Usage order list`: keeps most recent and least recent ordering
- `Eviction logic`: removes the least recently used key when full

### Data flow
1. A client reads or writes a key
2. The cache checks whether the key already exists
3. If found, the entry is moved to the most-recently-used position
4. If a new entry makes the cache exceed capacity, the least recently used entry is evicted
5. The client gets fast in-memory access for hot items

## Project structure

```text
Day-9/
  README.md
  lru-cache-sample/
    python/
      lru_cache.py
      demo.py
    java/
      LRUCache.java
      Main.java
```

## Run the sample

### Python

```powershell
cd lru-cache-sample\python
python demo.py
```

### Java

```powershell
cd lru-cache-sample\java
javac Main.java LRUCache.java
java Main
```

## Interview takeaways
- Caches improve latency, but invalidation and consistency are the real design problems
- LRU is simple and effective when recent access predicts near-future reuse
- A high hit rate matters more than adding a cache in principle
- Distributed caches trade local speed for shared state and operational complexity
- Cache stampedes and hot keys must be handled explicitly in production systems

## Next improvements
- Add TTL expiration support
- Add cache-aside loading behavior
- Add per-key locks to reduce stampedes
- Add metrics for hits, misses, and evictions
- Extend the sample into a Redis-backed caching layer
