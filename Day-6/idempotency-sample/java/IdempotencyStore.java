import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class IdempotencyStore<T> {
    private final Map<String, T> responses = new HashMap<>();

    public synchronized ProcessResult<T> execute(String key, Supplier<T> handler) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must be non-empty");
        }

        if (responses.containsKey(key)) {
            return new ProcessResult<>(responses.get(key), true);
        }

        T value = handler.get();
        responses.put(key, value);
        return new ProcessResult<>(value, false);
    }

    public synchronized int processedKeys() {
        return responses.size();
    }

    public record ProcessResult<T>(T value, boolean replayed) {
    }
}
