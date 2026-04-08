# Day 3: Databases, Consistency, and Partitioning

Day 3 focuses on how large systems store, protect, and distribute data.

It covers:
- databases and DBMS fundamentals
- SQL vs NoSQL trade-offs
- replication and indexes
- normalization and denormalization
- ACID, BASE, CAP, and PACELC
- transactions and distributed transactions
- sharding, consistent hashing, and federation

In short, Day 3 is about choosing the right data model and distribution strategy without ignoring consistency and operational cost.

![Day 3](./Day3.png)

## Core ideas

### Databases and DBMS
- A `database` stores structured or semi-structured data.
- A `DBMS` manages access, schema, indexing, durability, backup, and recovery.
- Tables, rows, columns, and schema matter even when the final system becomes distributed.

### SQL vs NoSQL
- `SQL` is strong for joins, transactions, and rigid schema.
- `NoSQL` is strong for flexible schema, scale-out workloads, and high write/read throughput.
- The right choice depends on workload, query shape, and consistency requirements.

### Replication and indexes
- Replication improves availability and read scaling, but introduces lag or conflict handling.
- Indexes improve read performance but increase write amplification and storage cost.

### Normalization vs denormalization
- `Normalization` reduces redundancy and improves consistency.
- `Denormalization` duplicates data to reduce joins and improve read performance.
- Most production systems use a balance of both.

### Consistency models
- `ACID` favors correctness and transactional guarantees.
- `BASE` favors availability and eventual convergence at scale.
- `CAP` explains the partition-time trade-off between consistency and availability.
- `PACELC` adds the normal-case latency vs consistency trade-off.

### Transactions and partitioning
- Single-node transactions are simpler.
- Distributed transactions need coordination like `2PC`, `3PC`, or sagas.
- `Sharding` spreads data across machines.
- `Consistent hashing` reduces rebalancing cost when shards are added or removed.

## Day-3 sample: Consistent Hashing Shard Router

This repository includes a small consistent hashing sample in both Python and Java.

### Functional requirements
- Add logical shard nodes to a hash ring
- Route a key to its owning shard
- Support virtual nodes for more even distribution
- Show how mappings change when a new shard is added

### High-level components
- `Client/API`: produces keys such as user IDs or order IDs
- `Hash ring`: maps keys to points on a ring
- `Shard nodes`: store the actual data partitions
- `Virtual nodes`: improve load distribution

### Data flow
1. Each shard is placed on the hash ring using multiple virtual nodes
2. A key is hashed into a ring position
3. The system walks clockwise to the next shard
4. That shard becomes the owner for the key
5. When a new shard is added, only a subset of keys move

## Project structure

```text
Day-3/
  README.md
  consistent-hashing-sample/
    python/
      consistent_hashing.py
      demo.py
    java/
      ConsistentHashRing.java
      Main.java
```

## Run the sample

### Python

```powershell
cd consistent-hashing-sample\python
python demo.py
```

### Java

```powershell
cd consistent-hashing-sample\java
javac Main.java ConsistentHashRing.java
java Main
```

## Interview takeaways
- SQL vs NoSQL is a workload decision, not a religion
- Replication improves resilience, but it changes failure behavior
- Sharding helps scale, but it complicates joins and rebalancing
- Consistent hashing is useful when nodes are expected to come and go
- CAP is a constraint, not an architecture recipe

## Next improvements
- Add replication factor to the hash ring
- Simulate read replicas and failover
- Add hot-key detection
- Compare simple modulo hashing vs consistent hashing
- Extend the sample into a sharded key-value store
