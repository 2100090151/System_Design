# Day 8: Search, Indexing, and Information Retrieval

Day 8 focuses on how large systems organize content so users can find the right documents, products, or records quickly.

It covers:
- search system basics
- inverted indexes and tokenization
- indexing pipeline and document updates
- ranking and relevance
- exact match vs full-text search
- autocomplete and prefix search
- sharding, replication, and query fan-out in search systems

In short, Day 8 is about turning large collections of data into something users can query efficiently and meaningfully.

![Day 8](./Day8.png)

## Core ideas

### Search basics
- A `search system` usually separates indexing from querying.
- During indexing, documents are parsed and transformed into a searchable structure.
- During querying, the system finds candidate documents and ranks them by relevance.

### Inverted index and tokenization
- An `inverted index` maps each term to the documents that contain it.
- `Tokenization` decides how text is split into searchable words or terms.
- Search quality depends heavily on normalization choices like lowercasing, stemming, stop-word removal, and punctuation handling.

### Indexing pipeline and updates
- Real systems often use an ingestion pipeline that validates, enriches, and indexes documents asynchronously.
- Updates are hard because search indexes must stay reasonably fresh without hurting query performance.
- Many systems use near-real-time indexing instead of fully synchronous updates.

### Ranking and relevance
- Search is not only about finding matches; it is also about ordering them well.
- Relevance can include term frequency, field weights, freshness, popularity, personalization, and business rules.
- Poor ranking makes a technically correct search engine feel broken to users.

### Autocomplete and distributed search
- `Autocomplete` often uses tries, prefix indexes, or cached popular prefixes.
- Large search systems shard indexes across machines and merge results from many shard responses.
- Replication improves availability, but it also creates consistency and freshness trade-offs.

## Day-8 sample: Inverted Index Search

This repository includes a small inverted index search sample in both Python and Java.

### Functional requirements
- Add documents to an index
- Tokenize document text into searchable terms
- Search for documents containing all query terms
- Keep the implementation simple enough for interview explanation

### High-level components
- `Document store`: holds the original content
- `Tokenizer`: normalizes and splits text into terms
- `Inverted index`: maps terms to document IDs
- `Query processor`: intersects matching document sets and returns results

### Data flow
1. A document is added to the system
2. The tokenizer normalizes and splits the text
3. Each term is written into the inverted index with its document ID
4. A user submits a query such as `distributed search`
5. The query processor finds matching posting lists and returns the documents that contain all query terms

## Project structure

```text
Day-8/
  README.md
  inverted-index-sample/
    python/
      inverted_index.py
      demo.py
    java/
      InvertedIndex.java
      Main.java
```

## Run the sample

### Python

```powershell
cd inverted-index-sample\python
python demo.py
```

### Java

```powershell
cd inverted-index-sample\java
javac Main.java InvertedIndex.java
java Main
```

## Interview takeaways
- Search systems are usually two systems in one: indexing and querying
- Inverted indexes are efficient, but tokenization and ranking decide quality
- Relevance is a product problem as much as an infrastructure problem
- Search freshness, shard fan-out, and query latency are common production trade-offs
- Autocomplete, typo tolerance, and ranking often matter more than raw matching speed

## Next improvements
- Add TF-IDF or BM25-style scoring
- Add phrase search and fielded search
- Add stop-word filtering and stemming
- Add prefix search for autocomplete
- Extend the sample into a tiny document search API
