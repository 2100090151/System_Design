# Day 13: Merkle Trees, Data Integrity, and Anti-Entropy

Day 13 focuses on how distributed systems compare large datasets efficiently without transferring every record.

It covers:
- Merkle trees and hierarchical hashing
- root hashes and integrity checks
- membership proofs
- replica comparison and anti-entropy
- synchronization in storage and blockchain-like systems
- hashing trade-offs and collision considerations

In short, Day 13 is about using tree-shaped hashes to detect divergence, verify inclusion, and synchronize distributed data efficiently.

![Day 13](./Day13.png)

[Day 13 PDF](./System_Design_Day_13.pdf)

## Core ideas

### Why Merkle trees matter
- Large replicas are expensive to compare record by record.
- A `Merkle tree` summarizes a dataset into a small root hash.
- If two replicas have the same root, their data matches for the same ordering and hash rules.

### Tree structure
- Leaves store hashes of raw values.
- Internal nodes store the hash of their two child hashes combined together.
- The topmost node is the `root hash`, which represents the whole dataset.

### Membership proofs
- A `Merkle proof` contains the sibling hashes needed to recompute the root from one leaf.
- This lets a client verify that one value belongs to a larger dataset without seeing every item.
- Membership proofs are useful when trust, bandwidth, or storage is limited.

### Anti-entropy and synchronization
- Replicas can compare root hashes first.
- If roots differ, they can compare subtree hashes to narrow the mismatch quickly.
- This avoids moving the full dataset when only a small subset changed.

### Trade-offs
- Tree results depend on deterministic ordering of input values.
- Updates can require recomputing hashes along the changed path.
- Hash choice matters for speed, security, and interoperability.

## Day-13 sample: Merkle Tree

This repository includes a small Merkle tree sample in both Python and Java.

### Functional requirements
- Build a Merkle tree from ordered values
- Compute the root hash
- Generate an inclusion proof for one leaf
- Verify the proof against a root hash
- Compare two trees to detect divergence

### High-level components
- `Leaf hashing`: turns each value into a deterministic digest
- `Parent hashing`: combines two child hashes into one parent
- `Merkle tree`: stores tree levels and exposes root/proof operations
- `Verifier`: recomputes the root from a value and its proof

### Data flow
1. A replica hashes each record into leaf hashes
2. Adjacent leaves are combined into parent hashes
3. The process repeats until one root hash remains
4. Another replica compares its root to detect whether the datasets match
5. A client can verify one value by replaying a proof back to the root

## Project structure

```text
Day-13/
  README.md
  merkle-tree-sample/
    python/
      merkle_tree.py
      demo.py
    java/
      MerkleTree.java
      Main.java
```

## Run the sample

### Python

```powershell
cd merkle-tree-sample\python
python demo.py
```

### Java

```powershell
cd merkle-tree-sample\java
javac Main.java MerkleTree.java
java Main
```

## Interview takeaways
- Merkle trees compress large datasets into a single comparable fingerprint
- Root mismatches tell you data changed, but subtree hashes help isolate where
- Inclusion proofs enable efficient verification without full data transfer
- Deterministic ordering is part of correctness, not just implementation detail
- Merkle trees appear in distributed databases, backup systems, replication protocols, and blockchain systems

## Next improvements
- Add subtree diffing to isolate the first mismatching branch
- Add streaming construction for very large datasets
- Add serialization for proofs and tree levels
- Support non-string payloads and custom hash functions
- Extend the sample into a replica repair workflow
