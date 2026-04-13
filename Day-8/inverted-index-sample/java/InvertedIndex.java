import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InvertedIndex {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-z0-9]+");

    private final Map<String, String> documents = new HashMap<>();
    private final Map<String, Set<String>> postings = new HashMap<>();

    public void addDocument(String documentId, String text) {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId must be non-empty");
        }
        if (documents.containsKey(documentId)) {
            throw new IllegalArgumentException("document already exists: " + documentId);
        }

        documents.put(documentId, text);
        Set<String> uniqueTerms = new HashSet<>(tokenize(text));
        for (String term : uniqueTerms) {
            postings.computeIfAbsent(term, ignored -> new HashSet<>()).add(documentId);
        }
    }

    public List<SearchResult> search(String query) {
        List<String> terms = tokenize(query);
        if (terms.isEmpty()) {
            return List.of();
        }

        Set<String> matchingIds = null;
        for (String term : terms) {
            Set<String> termPostings = postings.getOrDefault(term, Set.of());
            if (matchingIds == null) {
                matchingIds = new HashSet<>(termPostings);
            } else {
                matchingIds.retainAll(termPostings);
            }

            if (matchingIds.isEmpty()) {
                return List.of();
            }
        }

        List<String> sortedIds = new ArrayList<>(matchingIds);
        Collections.sort(sortedIds);

        List<SearchResult> results = new ArrayList<>();
        for (String documentId : sortedIds) {
            results.add(new SearchResult(documentId, documents.get(documentId)));
        }
        return results;
    }

    private static List<String> tokenize(String text) {
        Matcher matcher = TOKEN_PATTERN.matcher(text.toLowerCase());
        List<String> tokens = new ArrayList<>();
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    public record SearchResult(String documentId, String text) {
    }
}
