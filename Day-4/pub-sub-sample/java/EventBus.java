import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EventBus {
    @FunctionalInterface
    public interface EventHandler {
        void handle(String topic, Map<String, Object> payload);
    }

    private final Map<String, List<EventHandler>> subscribers;

    public EventBus() {
        this.subscribers = new HashMap<>();
    }

    public synchronized void subscribe(String topic, EventHandler handler) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Topic must not be empty");
        }

        subscribers.computeIfAbsent(topic, key -> new ArrayList<>()).add(handler);
    }

    public int publish(String topic, Map<String, Object> payload) {
        List<EventHandler> handlers;
        synchronized (this) {
            handlers = new ArrayList<>(subscribers.getOrDefault(topic, List.of()));
        }

        for (EventHandler handler : handlers) {
            handler.handle(topic, payload);
        }

        return handlers.size();
    }

    public synchronized Map<String, Integer> topicSnapshot() {
        Map<String, Integer> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, List<EventHandler>> entry : subscribers.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().size());
        }
        return snapshot;
    }
}
