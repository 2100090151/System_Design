from inverted_index import InvertedIndex


def main() -> None:
    index = InvertedIndex()

    index.add_document("doc-1", "Distributed search systems use inverted indexes for fast retrieval.")
    index.add_document("doc-2", "Distributed tracing helps debug request paths across services.")
    index.add_document("doc-3", "Search ranking decides which matching documents users see first.")
    index.add_document("doc-4", "An inverted index maps terms to the documents that contain them.")

    queries = [
        "distributed search",
        "inverted index",
        "ranking documents",
        "trace retrieval",
    ]

    for query in queries:
        print(f"query: {query}")
        results = index.search(query)
        if not results:
            print("  no matches")
            continue

        for document_id, text in results:
            print(f"  {document_id}: {text}")
        print()


if __name__ == "__main__":
    main()
