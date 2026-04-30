import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InboxConsumerSystem {
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

    public static final class InboxStore {
        private final Set<String> processed = new HashSet<>();

        public boolean exists(String messageId) {
            return processed.contains(messageId);
        }

        public void add(String messageId) {
            processed.add(messageId);
        }
    }

    public static final class LedgerStore {
        private final Map<String, Integer> balances = new HashMap<>();

        public void apply(String accountId, int amountCents) {
            balances.put(accountId, balances.getOrDefault(accountId, 0) + amountCents);
        }

        public int balance(String accountId) {
            return balances.getOrDefault(accountId, 0);
        }
    }

    public static final class InboxConsumer {
        private final InboxStore inbox;
        private final LedgerStore ledger;
        private int appliedCount = 0;
        private int duplicateCount = 0;

        public InboxConsumer(InboxStore inbox, LedgerStore ledger) {
            this.inbox = inbox;
            this.ledger = ledger;
        }

        public String process(Message message) {
            if (inbox.exists(message.getMessageId())) {
                duplicateCount += 1;
                return "duplicate skipped " + message.getMessageId();
            }

            ledger.apply(message.getAccountId(), message.getAmountCents());
            inbox.add(message.getMessageId());
            appliedCount += 1;
            return "applied " + message.getMessageId() + " amount=" + message.getAmountCents();
        }

        public int balance(String accountId) {
            return ledger.balance(accountId);
        }

        public int getAppliedCount() {
            return appliedCount;
        }

        public int getDuplicateCount() {
            return duplicateCount;
        }
    }
}
