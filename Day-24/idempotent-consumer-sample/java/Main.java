import java.util.List;

public class Main {
    public static void main(String[] args) {
        IdempotentConsumerSystem.DedupStore dedup = new IdempotentConsumerSystem.DedupStore();
        IdempotentConsumerSystem.BalanceLedger ledger = new IdempotentConsumerSystem.BalanceLedger();
        IdempotentConsumerSystem.PaymentConsumer consumer = new IdempotentConsumerSystem.PaymentConsumer(dedup, ledger);

        List<IdempotentConsumerSystem.Message> messages = List.of(
            new IdempotentConsumerSystem.Message("msg-1", "acct-1", 1500),
            new IdempotentConsumerSystem.Message("msg-2", "acct-1", 2500),
            new IdempotentConsumerSystem.Message("msg-1", "acct-1", 1500),
            new IdempotentConsumerSystem.Message("msg-3", "acct-1", -700),
            new IdempotentConsumerSystem.Message("msg-2", "acct-1", 2500),
            new IdempotentConsumerSystem.Message("msg-4", "acct-2", 900)
        );

        for (IdempotentConsumerSystem.Message message : messages) {
            System.out.println(consumer.handle(message));
        }

        System.out.println("acct-1 balance: " + consumer.balance("acct-1"));
        System.out.println("acct-2 balance: " + consumer.balance("acct-2"));
        System.out.println("applied_count: " + consumer.getAppliedCount());
        System.out.println("duplicate_count: " + consumer.getDuplicateCount());
    }
}
