# Day 12: Probabilistic Data Structures and Bloom Filters

Day 12 focuses on how large systems answer approximate questions quickly when exact answers would cost too much memory or latency.

It covers:
- probabilistic data structures
- approximate membership checks
- Bloom filters and false positives
- memory vs accuracy trade-offs
- cache penetration and duplicate suppression
- cardinality estimation and sketch-based thinking

In short, Day 12 is about using approximation deliberately so systems can stay fast and cheap at scale.

![Day 12](./Day12.png)

[Day 12 PDF](./System_Design_Day_12.pdf)

## Core ideas

### Why probabilistic data structures matter
- Exact indexes can become expensive when the dataset is huge.
- Probabilistic structures trade a small amount of accuracy for major memory savings.
- They are useful when a fast "probably yes" or "definitely no" answer is enough.

### Bloom filter basics
- A `Bloom filter` is a bit array plus multiple hash functions.
- Adding an item sets several bit positions.
- Checking an item returns `definitely not present` if any required bit is unset, or `might be present` if all bits are set.

### False positives and no false negatives
- A Bloom filter can say an item `might exist` even when it does not.
- A correctly implemented standard Bloom filter should not produce false negatives for added items.
- As more items are inserted, the false-positive rate increases.

### Where Bloom filters help
- Prevent unnecessary database reads for definitely missing keys
- Filter duplicate events before expensive downstream work
- Reduce disk lookups in storage engines
- Pre-check membership in distributed caches or search systems

### Trade-offs
- Bloom filters do not store the original values.
- Standard Bloom filters do not support exact deletion.
- You need to size the bit array and hash count based on expected volume and acceptable false-positive rate.

## Day-12 sample: Bloom Filter

This repository includes a small Bloom filter sample in both Python and Java.

### Functional requirements
- Create a Bloom filter with configurable size and hash count
- Add items into the filter
- Check whether an item might already exist
- Estimate the current false-positive probability

### High-level components
- `Client`: adds and checks keys
- `Bit array`: compact storage for membership markers
- `Hash functions`: map each item to multiple bit positions
- `Bloom filter`: coordinates writes, reads, and statistics

### Data flow
1. A client adds an item such as `user:1001`
2. The Bloom filter computes multiple hash indexes for that item
3. It sets the corresponding bits in the bit array
4. A later lookup computes the same indexes
5. If any bit is unset, the item is definitely absent
6. If all bits are set, the item might be present and the caller can decide whether to trust the result or do an exact lookup

## Project structure

```text
Day-12/
  README.md
  bloom-filter-sample/
    python/
      bloom_filter.py
      demo.py
    java/
      BloomFilter.java
      Main.java
```

## Run the sample

### Python

```powershell
cd bloom-filter-sample\python
python demo.py
```

### Java

```powershell
cd bloom-filter-sample\java
javac Main.java BloomFilter.java
java Main
```

## Interview takeaways
- Bloom filters are useful when "definitely not present" is more valuable than exact membership
- Memory efficiency comes from storing hashed bit positions instead of original values
- False positives grow as the filter fills, so capacity planning matters
- Bloom filters are often paired with an exact store such as Redis, SSTables, or a database
- Approximate data structures are valuable because system design often rewards bounded error over unbounded cost

## Next improvements
- Add optimal-size calculation from expected item count and target false-positive rate
- Add a counting Bloom filter variant for deletions
- Add serialization for storing and loading filters
- Add benchmark code for fill ratio and false-positive experiments
- Extend the sample into a cache-miss protection layer
