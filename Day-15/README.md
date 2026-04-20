# Day 15: Raft Consensus, Leader Election, and Log Replication

Day 15 focuses on how distributed systems agree on one ordered history of updates despite failures.

It covers:
- consensus fundamentals
- Raft roles and terms
- leader election and majority voting
- replicated logs and commit rules
- heartbeats and leadership stability
- safety vs availability trade-offs under partitions

In short, Day 15 is about coordinating multiple nodes so they behave like one consistent state machine.

![Day 15](./Day15.png)

[Day 15 PDF](./System_Design_Day_15.pdf)

## Core ideas

### Why consensus matters
- Replication alone does not guarantee correctness when nodes disagree.
- A `consensus protocol` ensures a single, agreed order of operations.
- This prevents split-brain behavior and conflicting committed history.

### Raft roles and terms
- Nodes move between `Follower`, `Candidate`, and `Leader` roles.
- Time is divided into logical `terms`.
- At most one leader should be elected per term by majority vote.

### Leader election
- A candidate increments term and requests votes from peers.
- Each node votes once per term for an eligible candidate.
- Candidate with majority becomes leader; others follow that leader.

### Log replication and commit
- Clients write through the leader.
- Leader appends an entry locally, then replicates to followers.
- Entry is committed when a majority acknowledges it in the leader term.

### Safety and trade-offs
- Majority rules protect safety but can reduce availability during failures.
- Minority partitions should not make progress.
- Performance depends on quorum size, network latency, and batching strategy.

## Day-15 sample: Raft Consensus (Simplified)

This repository includes a small Raft-style sample in both Python and Java.

### Functional requirements
- Elect a leader via majority voting
- Reject stale leader/candidate terms
- Append commands through the elected leader
- Replicate entries to followers
- Commit entries only after majority acknowledgement

### High-level components
- `RaftNode`: term, vote, role, and replicated log
- `RaftCluster`: election and replication coordinator
- `RequestVote`: election RPC behavior
- `AppendEntries`: heartbeat/log replication behavior

### Data flow
1. A node starts election and requests votes
2. On majority votes, it becomes leader
3. Client sends command to the leader
4. Leader appends and replicates the entry to followers
5. On majority acknowledgements, leader commits the entry

## Project structure

```text
Day-15/
  README.md
  raft-consensus-sample/
    python/
      raft_cluster.py
      demo.py
    java/
      RaftCluster.java
      Main.java
```

## Run the sample

### Python

```powershell
cd raft-consensus-sample\python
python demo.py
```

### Java

```powershell
cd raft-consensus-sample\java
javac Main.java RaftCluster.java
java Main
```

## Interview takeaways
- Consensus is about ordered agreement, not just copying data
- Majority quorum is the core safety boundary in Raft
- Terms prevent stale leaders from overwriting newer history
- Leader-based replication simplifies reasoning about write order
- Commit rules must be explicit, otherwise replicated logs can still diverge

## Next improvements
- Add randomized election timeouts and heartbeat timers
- Add log conflict resolution with follower rollback
- Add persistent state and restart recovery
- Add snapshotting and log compaction
- Simulate network partitions and rejoin behavior
