import java.util.List;

public class Main {
    public static void main(String[] args) {
        InboxConsumerSystem.InboxStore inbox = new InboxConsumerSystem.InboxStore();
        InboxConsumerSystem.LedgerStore ledger = new InboxConsumerSystem.LedgerStore();
        InboxConsumerSystem.InboxConsumer consumer = new InboxConsumerSystem.InboxConsumer(inbox, ledger);

        List<InboxConsumerSystem.Message> deliveries = List.of(
            new InboxConsumerSystem.Message("m-100", "acct-A", 1200),
            new InboxConsumerSystem.Message("m-101", "acct-A", 800),
            new InboxConsumerSystem.Message("m-100", "acct-A", 1200),
            new InboxConsumerSystem.Message("m-102", "acct-A", -300),
            new InboxConsumerSystem.Message("m-103", "acct-B", 900),
            new InboxConsumerSystem.Message("m-101", "acct-A", 800)
        );

        for (InboxConsumerSystem.Message message : deliveries) {
            System.out.println(consumer.process(message));
        }

        System.out.println("acct-A balance: " + consumer.balance("acct-A"));
        System.out.println("acct-B balance: " + consumer.balance("acct-B"));
        System.out.println("applied_count: " + consumer.getAppliedCount());
        System.out.println("duplicate_count: " + consumer.getDuplicateCount());
    }
}
