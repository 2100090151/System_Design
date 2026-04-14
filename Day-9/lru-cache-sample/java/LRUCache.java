import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<K, V> {
    private final int capacity;
    private final LinkedHashMap<K, V> items;

    public LRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.items = new LinkedHashMap<>(16, 0.75f, true);
    }

    public V get(K key) {
        return items.get(key);
    }

    public K put(K key, V value) {
        items.put(key, value);
        if (items.size() > capacity) {
            K evictedKey = items.keySet().iterator().next();
            items.remove(evictedKey);
            return evictedKey;
        }
        return null;
    }

    public Map<K, V> snapshot() {
        return new LinkedHashMap<>(items);
    }
}
