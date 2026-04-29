import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class IdempotentConsumerSystem {
    public static final class Message {
        private final String messageId;
        private final String accountId;
        private final int amountCents;

        public Message(String messageId, String accountId, int amountCents) {
            this.messageId = messageId;
            this.accountId = accountId;
            this.amountCents = amountCents;
        }

        public String getMessageId() {
            return messageId;
        }

        public String getAccountId() {
            return accountId;
        }

        public int getAmountCents() {
            return amountCents;
        }
    }

    public static final class DedupStore {
        private final Set<String> seen = new HashSet<>();

        public boolean hasSeen(String messageId) {
            return seen.contains(messageId);
        }

        public void markSeen(String messageId) {
            seen.add(messageId);
        }
    }

    public static final class BalanceLedger {
        private final Map<String, Integer> balances = new HashMap<>();

        public void apply(String accountId, int amountCents) {
            balances.put(accountId, balances.getOrDefault(accountId, 0) + amountCents);
        }

        public int getBalance(String accountId) {
            return balances.getOrDefault(accountId, 0);
        }
    }

    public static final class PaymentConsumer {
        private final DedupStore dedupStore;
        private final BalanceLedger ledger;
        private int appliedCount = 0;
        private int duplicateCount = 0;

        public PaymentConsumer(DedupStore dedupStore, BalanceLedger ledger) {
            this.dedupStore = dedupStore;
            this.ledger = ledger;
        }

        public String handle(Message message) {
            if (dedupStore.hasSeen(message.getMessageId())) {
                duplicateCount += 1;
                return "skipped duplicate " + message.getMessageId();
            }

            ledger.apply(message.getAccountId(), message.getAmountCents());
            dedupStore.markSeen(message.getMessageId());
            appliedCount += 1;
            return "applied " + message.getMessageId() + " amount=" + message.getAmountCents();
        }

        public int balance(String accountId) {
            return ledger.getBalance(accountId);
        }

        public int getAppliedCount() {
            return appliedCount;
        }

        public int getDuplicateCount() {
            return duplicateCount;
        }
    }
}
