import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TransactionalOutbox {
    public enum OutboxStatus {
        PENDING,
        SENT
    }

    public static final class Order {
        private final String orderId;
        private final int amountCents;

        public Order(String orderId, int amountCents) {
            this.orderId = orderId;
            this.amountCents = amountCents;
        }

        public String getOrderId() {
            return orderId;
        }

        public int getAmountCents() {
            return amountCents;
        }
    }

    public static final class OutboxMessage {
        private final int messageId;
        private final String aggregateId;
        private final String topic;
        private final String payload;
        private OutboxStatus status;
        private int attempts;

        public OutboxMessage(int messageId, String aggregateId, String topic, String payload) {
            this.messageId = messageId;
            this.aggregateId = aggregateId;
            this.topic = topic;
            this.payload = payload;
            this.status = OutboxStatus.PENDING;
            this.attempts = 0;
        }

        public int getMessageId() {
            return messageId;
        }

        public String getAggregateId() {
            return aggregateId;
        }

        public String getTopic() {
            return topic;
        }

        public String getPayload() {
            return payload;
        }

        public OutboxStatus getStatus() {
            return status;
        }

        public int getAttempts() {
            return attempts;
        }

        private void markSent() {
            status = OutboxStatus.SENT;
        }

        private void markFailure() {
            attempts += 1;
        }

        public OutboxMessage copy() {
            OutboxMessage cloned = new OutboxMessage(messageId, aggregateId, topic, payload);
            cloned.status = status;
            cloned.attempts = attempts;
            return cloned;
        }
    }

    public static final class InMemoryStore {
        private final Map<String, Order> orders = new LinkedHashMap<>();
        private final List<OutboxMessage> outbox = new ArrayList<>();
        private int nextMessageId = 1;

        public Transaction beginTransaction() {
            return new Transaction(this);
        }

        public List<OutboxMessage> pendingMessages(int limit) {
            if (limit <= 0) {
                return List.of();
            }
            List<OutboxMessage> pending = new ArrayList<>();
            for (OutboxMessage message : outbox) {
                if (message.getStatus() == OutboxStatus.PENDING) {
                    pending.add(message);
                    if (pending.size() >= limit) {
                        break;
                    }
                }
            }
            return pending;
        }

        public void markSent(int messageId) {
            findMessage(messageId).markSent();
        }

        public void markFailure(int messageId) {
            findMessage(messageId).markFailure();
        }

        public Map<String, Order> ordersSnapshot() {
            return new LinkedHashMap<>(orders);
        }

        public List<OutboxMessage> outboxSnapshot() {
            List<OutboxMessage> snapshot = new ArrayList<>();
            for (OutboxMessage message : outbox) {
                snapshot.add(message.copy());
            }
            return snapshot;
        }

        private int allocateMessageId() {
            int id = nextMessageId;
            nextMessageId += 1;
            return id;
        }

        private OutboxMessage findMessage(int messageId) {
            for (OutboxMessage message : outbox) {
                if (message.getMessageId() == messageId) {
                    return message;
                }
            }
            throw new IllegalArgumentException("unknown message_id: " + messageId);
        }
    }

    public static final class Transaction {
        private final InMemoryStore store;
        private Order stagedOrder;
        private OutboxMessage stagedEvent;

        private Transaction(InMemoryStore store) {
            this.store = store;
        }

        public void createOrder(String orderId, int amountCents) {
            if (store.orders.containsKey(orderId)) {
                throw new IllegalArgumentException("order already exists: " + orderId);
            }
            if (amountCents <= 0) {
                throw new IllegalArgumentException("amountCents must be positive");
            }

            stagedOrder = new Order(orderId, amountCents);
            stagedEvent = new OutboxMessage(
                store.allocateMessageId(),
                orderId,
                "orders.created",
                "{'order_id':'" + orderId + "','amount_cents':" + amountCents + "}"
            );
        }

        public void commit(boolean simulateDbFailure) {
            if (stagedOrder == null || stagedEvent == null) {
                throw new IllegalStateException("transaction has no staged work");
            }
            if (simulateDbFailure) {
                throw new IllegalStateException("simulated database failure before commit");
            }

            store.orders.put(stagedOrder.getOrderId(), stagedOrder);
            store.outbox.add(stagedEvent);
        }
    }

    public interface Broker {
        boolean publish(String topic, String payload);
    }

    public static final class UnreliableBroker implements Broker {
        private final Set<Integer> failOnCalls;
        private final List<String> published = new ArrayList<>();
        private int callCount = 0;

        public UnreliableBroker(Set<Integer> failOnCalls) {
            this.failOnCalls = new LinkedHashSet<>(failOnCalls);
        }

        @Override
        public boolean publish(String topic, String payload) {
            callCount += 1;
            if (failOnCalls.contains(callCount)) {
                return false;
            }
            published.add(topic + ":" + payload);
            return true;
        }

        public List<String> getPublished() {
            return List.copyOf(published);
        }
    }

    public static final class OutboxRelay {
        private final InMemoryStore store;
        private final Broker broker;

        public OutboxRelay(InMemoryStore store, Broker broker) {
            this.store = store;
            this.broker = broker;
        }

        public List<String> pollAndPublish(int limit) {
            List<String> history = new ArrayList<>();
            for (OutboxMessage message : store.pendingMessages(limit)) {
                boolean ok = broker.publish(message.getTopic(), message.getPayload());
                if (ok) {
                    store.markSent(message.getMessageId());
                    history.add("published message " + message.getMessageId());
                } else {
                    store.markFailure(message.getMessageId());
                    history.add(
                        "publish failed for message "
                            + message.getMessageId()
                            + "; will retry"
                    );
                }
            }
            return history;
        }
    }
}
