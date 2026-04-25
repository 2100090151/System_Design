import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CqrsOrderSystem {
    public enum OrderStatus {
        CREATED,
        CONFIRMED
    }

    public static final class OrderAggregate {
        private final String orderId;
        private final String customerId;
        private final int amountCents;
        private final OrderStatus status;

        public OrderAggregate(String orderId, String customerId, int amountCents, OrderStatus status) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.amountCents = amountCents;
            this.status = status;
        }

        public String getOrderId() {
            return orderId;
        }

        public String getCustomerId() {
            return customerId;
        }

        public int getAmountCents() {
            return amountCents;
        }

        public OrderStatus getStatus() {
            return status;
        }
    }

    public static final class DomainEvent {
        private final int sequence;
        private final String eventType;
        private final String orderId;
        private final String customerId;
        private final int amountCents;

        public DomainEvent(
            int sequence,
            String eventType,
            String orderId,
            String customerId,
            int amountCents
        ) {
            this.sequence = sequence;
            this.eventType = eventType;
            this.orderId = orderId;
            this.customerId = customerId;
            this.amountCents = amountCents;
        }

        public int getSequence() {
            return sequence;
        }

        public String getEventType() {
            return eventType;
        }

        public String getOrderId() {
            return orderId;
        }

        public String getCustomerId() {
            return customerId;
        }

        public int getAmountCents() {
            return amountCents;
        }
    }

    public static final class OrderReadModel {
        private final String orderId;
        private final String customerId;
        private final int amountCents;
        private String status;

        public OrderReadModel(String orderId, String customerId, int amountCents, String status) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.amountCents = amountCents;
            this.status = status;
        }

        public String getOrderId() {
            return orderId;
        }

        public String getCustomerId() {
            return customerId;
        }

        public int getAmountCents() {
            return amountCents;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static final class WriteStore {
        private final Map<String, OrderAggregate> orders = new LinkedHashMap<>();
        private final List<DomainEvent> events = new ArrayList<>();
        private int nextSequence = 1;

        public DomainEvent appendEvent(
            String eventType,
            String orderId,
            String customerId,
            int amountCents
        ) {
            DomainEvent event = new DomainEvent(
                nextSequence,
                eventType,
                orderId,
                customerId,
                amountCents
            );
            nextSequence += 1;
            events.add(event);
            return event;
        }

        public Map<String, OrderAggregate> getOrders() {
            return orders;
        }

        public List<DomainEvent> getEvents() {
            return events;
        }
    }

    public static final class ReadStore {
        private final Map<String, OrderReadModel> orders = new LinkedHashMap<>();

        public Map<String, OrderReadModel> getOrders() {
            return orders;
        }
    }

    public static final class CommandService {
        private final WriteStore writeStore;

        public CommandService(WriteStore writeStore) {
            this.writeStore = writeStore;
        }

        public void createOrder(String orderId, String customerId, int amountCents) {
            if (writeStore.getOrders().containsKey(orderId)) {
                throw new IllegalArgumentException("order already exists: " + orderId);
            }
            if (amountCents <= 0) {
                throw new IllegalArgumentException("amountCents must be positive");
            }

            OrderAggregate aggregate = new OrderAggregate(
                orderId,
                customerId,
                amountCents,
                OrderStatus.CREATED
            );
            writeStore.getOrders().put(orderId, aggregate);
            writeStore.appendEvent("OrderCreated", orderId, customerId, amountCents);
        }

        public void confirmOrder(String orderId) {
            OrderAggregate aggregate = writeStore.getOrders().get(orderId);
            if (aggregate == null) {
                throw new IllegalArgumentException("order not found: " + orderId);
            }
            if (aggregate.getStatus() == OrderStatus.CONFIRMED) {
                return;
            }

            OrderAggregate confirmed = new OrderAggregate(
                aggregate.getOrderId(),
                aggregate.getCustomerId(),
                aggregate.getAmountCents(),
                OrderStatus.CONFIRMED
            );
            writeStore.getOrders().put(orderId, confirmed);
            writeStore.appendEvent(
                "OrderConfirmed",
                orderId,
                aggregate.getCustomerId(),
                aggregate.getAmountCents()
            );
        }
    }

    public static final class Projector {
        private final WriteStore writeStore;
        private final ReadStore readStore;
        private int lastSequence = 0;

        public Projector(WriteStore writeStore, ReadStore readStore) {
            this.writeStore = writeStore;
            this.readStore = readStore;
        }

        public int getLastSequence() {
            return lastSequence;
        }

        public List<String> projectNext(int batchSize) {
            List<String> history = new ArrayList<>();
            if (batchSize <= 0) {
                return history;
            }

            int processed = 0;
            for (DomainEvent event : writeStore.getEvents()) {
                if (event.getSequence() <= lastSequence) {
                    continue;
                }
                if (processed >= batchSize) {
                    break;
                }

                if ("OrderCreated".equals(event.getEventType())) {
                    readStore.getOrders().put(
                        event.getOrderId(),
                        new OrderReadModel(
                            event.getOrderId(),
                            event.getCustomerId(),
                            event.getAmountCents(),
                            OrderStatus.CREATED.name()
                        )
                    );
                    history.add("projected " + event.getEventType() + "#" + event.getSequence());
                } else if ("OrderConfirmed".equals(event.getEventType())) {
                    OrderReadModel row = readStore.getOrders().get(event.getOrderId());
                    if (row != null) {
                        row.setStatus(OrderStatus.CONFIRMED.name());
                    }
                    history.add("projected " + event.getEventType() + "#" + event.getSequence());
                } else {
                    history.add("skipped unknown event#" + event.getSequence());
                }

                lastSequence = event.getSequence();
                processed += 1;
            }

            return history;
        }
    }

    public static final class QueryService {
        private final ReadStore readStore;

        public QueryService(ReadStore readStore) {
            this.readStore = readStore;
        }

        public OrderReadModel getOrder(String orderId) {
            OrderReadModel row = readStore.getOrders().get(orderId);
            if (row == null) {
                return null;
            }
            return new OrderReadModel(
                row.getOrderId(),
                row.getCustomerId(),
                row.getAmountCents(),
                row.getStatus()
            );
        }

        public List<OrderReadModel> listOrdersForCustomer(String customerId) {
            List<OrderReadModel> result = new ArrayList<>();
            for (OrderReadModel row : readStore.getOrders().values()) {
                if (!customerId.equals(row.getCustomerId())) {
                    continue;
                }
                result.add(
                    new OrderReadModel(
                        row.getOrderId(),
                        row.getCustomerId(),
                        row.getAmountCents(),
                        row.getStatus()
                    )
                );
            }
            return result;
        }
    }
}
