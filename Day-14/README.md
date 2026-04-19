# Day 14: Quorum Replication, Tunable Consistency, and Read Repair

Day 14 focuses on how replicated databases balance consistency and availability using quorum reads/writes.

It covers:
- replication factor and quorum math (`N`, `R`, `W`)
- tunable consistency levels
- stale reads and version selection
- read repair for eventual convergence
- availability trade-offs under replica failures
- practical interview reasoning for Dynamo-style systems

In short, Day 14 is about serving reads and writes from replicated nodes while controlling correctness with quorum choices.

![Day 14](./Day14.png)
[Day 14 PDF](./System_Design_Day_14.pdf)

## Core ideas

### Quorum fundamentals
- `N` is the number of replicas that store each key.
- `W` is the minimum replicas required to accept a write.
- `R` is the minimum replicas required to serve a read.
- If `R + W > N`, read/write quorums overlap, which improves the chance of reading the latest value.

### Tunable consistency
- Systems often let clients choose consistency levels per request.
- Stronger consistency uses larger `R`/`W`, but lowers availability and increases latency.
- Weaker consistency improves availability and latency, but can return stale data.

### Versioned values
- Replicas can temporarily diverge due to failures or lag.
- A read collects multiple versions and picks the newest version.
- Version metadata (logical version/timestamp/vector clock) is required to resolve conflicts.

### Read repair
- After a read finds the newest version, stale replicas can be repaired in the same path.
- Read repair improves eventual convergence without a full anti-entropy cycle.
- Repair-on-read is best for hot keys; cold keys still need background repair.

## Day-14 sample: Quorum Key-Value Store

This repository includes a small quorum-based key-value sample in both Python and Java.

### Functional requirements
- Write a key/value with configurable write quorum (`W`)
- Read a key/value with configurable read quorum (`R`)
- Track per-key versions to choose the latest value
- Simulate replica availability changes
- Perform read repair on stale replicas

### High-level components
- `Replica`: in-memory key-value copy with availability state
- `Versioned value`: payload plus monotonic version
- `Quorum store`: coordinates reads/writes across replicas
- `Repair logic`: updates stale replicas after a read

### Data flow
1. A write request increments the key version and replicates to available nodes
2. If available nodes are below `W`, write is rejected
3. A read gathers responses from available nodes
4. If available nodes are below `R`, read is rejected
5. The newest version is returned and stale responders are repaired

## Project structure

```text
Day-14/
  README.md
  quorum-kv-sample/
    python/
      quorum_kv.py
      demo.py
    java/
      QuorumKVStore.java
      Main.java
```

## Run the sample

### Python

```powershell
cd quorum-kv-sample\python
python demo.py
```

### Java

```powershell
cd quorum-kv-sample\java
javac Main.java QuorumKVStore.java
java Main
```

## Interview takeaways
- Quorum settings are a business decision across consistency, latency, and availability
- `R + W > N` is a useful rule, not a complete guarantee without sane versioning/clock strategy
- Replica failures create divergence; read repair helps but does not replace background anti-entropy
- Consistency models should be explicit in API contracts, not implicit implementation detail
- Dynamo-style designs prioritize graceful degradation and eventual convergence under failure

## Next improvements
- Add sloppy quorum and hinted handoff simulation
- Add vector clocks for concurrent write conflict resolution
- Add background anti-entropy loop independent of reads
- Add per-request consistency overrides (`ONE`, `QUORUM`, `ALL`)
- Add latency modeling to show user-visible trade-offs for each consistency level
