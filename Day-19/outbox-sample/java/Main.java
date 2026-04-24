import java.util.Map;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        TransactionalOutbox.InMemoryStore store = new TransactionalOutbox.InMemoryStore();

        TransactionalOutbox.Transaction tx1 = store.beginTransaction();
        tx1.createOrder("order-100", 2599);
        tx1.commit(false);

        TransactionalOutbox.Transaction tx2 = store.beginTransaction();
        tx2.createOrder("order-101", 1299);
        try {
            tx2.commit(true);
        } catch (IllegalStateException error) {
            System.out.println("simulated failure: " + error.getMessage());
        }

        System.out.println("\nstate after transactions");
        printState(store);

        TransactionalOutbox.UnreliableBroker broker = new TransactionalOutbox.UnreliableBroker(Set.of(1));
        TransactionalOutbox.OutboxRelay relay = new TransactionalOutbox.OutboxRelay(store, broker);

        System.out.println("\nrelay pass 1");
        for (String line : relay.pollAndPublish(10)) {
            System.out.println("  " + line);
        }
        printState(store);

        System.out.println("\nrelay pass 2");
        for (String line : relay.pollAndPublish(10)) {
            System.out.println("  " + line);
        }
        printState(store);
    }

    private static void printState(TransactionalOutbox.InMemoryStore store) {
        System.out.println("orders:");
        for (Map.Entry<String, TransactionalOutbox.Order> entry : store.ordersSnapshot().entrySet()) {
            System.out.println(
                "  "
                    + entry.getKey()
                    + " amount="
                    + entry.getValue().getAmountCents()
            );
        }

        System.out.println("outbox:");
        for (TransactionalOutbox.OutboxMessage message : store.outboxSnapshot()) {
            System.out.println(
                "  id="
                    + message.getMessageId()
                    + " aggregate="
                    + message.getAggregateId()
                    + " status="
                    + message.getStatus()
                    + " attempts="
                    + message.getAttempts()
            );
        }
    }
}
