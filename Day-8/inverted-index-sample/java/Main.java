import java.util.List;

public class Main {
    public static void main(String[] args) {
        InvertedIndex index = new InvertedIndex();

        index.addDocument("doc-1", "Distributed search systems use inverted indexes for fast retrieval.");
        index.addDocument("doc-2", "Distributed tracing helps debug request paths across services.");
        index.addDocument("doc-3", "Search ranking decides which matching documents users see first.");
        index.addDocument("doc-4", "An inverted index maps terms to the documents that contain them.");

        runQuery(index, "distributed search");
        runQuery(index, "inverted index");
        runQuery(index, "ranking documents");
        runQuery(index, "trace retrieval");
    }

    private static void runQuery(InvertedIndex index, String query) {
        System.out.println("query: " + query);
        List<InvertedIndex.SearchResult> results = index.search(query);
        if (results.isEmpty()) {
            System.out.println("  no matches");
            return;
        }

        for (InvertedIndex.SearchResult result : results) {
            System.out.printf("  %s: %s%n", result.documentId(), result.text());
        }
        System.out.println();
    }
}
