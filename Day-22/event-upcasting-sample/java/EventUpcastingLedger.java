import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EventUpcastingLedger {
    public static final class RawEvent {
        private final int sequence;
        private final String accountId;
        private final String eventType;
        private final int schemaVersion;
        private final Map<String, Object> payload;

        public RawEvent(
            int sequence,
            String accountId,
            String eventType,
            int schemaVersion,
            Map<String, Object> payload
        ) {
            this.sequence = sequence;
            this.accountId = accountId;
            this.eventType = eventType;
            this.schemaVersion = schemaVersion;
            this.payload = payload;
        }

        public int getSequence() {
            return sequence;
        }

        public String getAccountId() {
            return accountId;
        }

        public String getEventType() {
            return eventType;
        }

        public int getSchemaVersion() {
            return schemaVersion;
        }

        public Map<String, Object> getPayload() {
            return payload;
        }
    }

    public static final class CanonicalEvent {
        private final int sequence;
        private final String accountId;
        private final String eventType;
        private final int schemaVersion;
        private final int amountCents;
        private final String currency;

        public CanonicalEvent(
            int sequence,
            String accountId,
            String eventType,
            int schemaVersion,
            int amountCents,
            String currency
        ) {
            this.sequence = sequence;
            this.accountId = accountId;
            this.eventType = eventType;
            this.schemaVersion = schemaVersion;
            this.amountCents = amountCents;
            this.currency = currency;
        }

        public int getSequence() {
            return sequence;
        }

        public String getAccountId() {
            return accountId;
        }

        public String getEventType() {
            return eventType;
        }

        public int getSchemaVersion() {
            return schemaVersion;
        }

        public int getAmountCents() {
            return amountCents;
        }

        public String getCurrency() {
            return currency;
        }
    }

    public static final class EventStore {
        private final List<RawEvent> events = new ArrayList<>();
        private int nextSequence = 1;

        public RawEvent append(
            String accountId,
            String eventType,
            int schemaVersion,
            Map<String, Object> payload
        ) {
            if (accountId == null || accountId.isBlank()) {
                throw new IllegalArgumentException("accountId must be non-empty");
            }
            if (eventType == null || eventType.isBlank()) {
                throw new IllegalArgumentException("eventType must be non-empty");
            }
            if (schemaVersion <= 0) {
                throw new IllegalArgumentException("schemaVersion must be positive");
            }

            RawEvent event = new RawEvent(
                nextSequence,
                accountId,
                eventType,
                schemaVersion,
                new LinkedHashMap<>(payload)
            );
            events.add(event);
            nextSequence += 1;
            return event;
        }

        public List<RawEvent> eventsForAccount(String accountId) {
            List<RawEvent> result = new ArrayList<>();
            for (RawEvent event : events) {
                if (accountId.equals(event.getAccountId())) {
                    result.add(event);
                }
            }
            return result;
        }
    }

    public static final class EventUpcaster {
        private final Map<String, Integer> latestVersionByType = new LinkedHashMap<>();

        public EventUpcaster() {
            latestVersionByType.put("FundsDeposited", 2);
            latestVersionByType.put("FundsWithdrawn", 2);
        }

        public CanonicalEvent upcast(RawEvent event) {
            Integer latest = latestVersionByType.get(event.getEventType());
            if (latest == null) {
                throw new IllegalArgumentException("unsupported eventType: " + event.getEventType());
            }
            if (event.getSchemaVersion() > latest) {
                throw new IllegalArgumentException(
                    "unsupported future schema for "
                        + event.getEventType()
                        + ": "
                        + event.getSchemaVersion()
                        + " > "
                        + latest
                );
            }

            int currentVersion = event.getSchemaVersion();
            Map<String, Object> payload = new LinkedHashMap<>(event.getPayload());

            while (currentVersion < latest) {
                payload = upcastOnce(event.getEventType(), currentVersion, payload);
                currentVersion += 1;
            }

            int amountCents = ((Number) payload.get("amount_cents")).intValue();
            String currency = payload.get("currency").toString();

            return new CanonicalEvent(
                event.getSequence(),
                event.getAccountId(),
                event.getEventType(),
                currentVersion,
                amountCents,
                currency
            );
        }

        private Map<String, Object> upcastOnce(
            String eventType,
            int fromVersion,
            Map<String, Object> payload
        ) {
            if (fromVersion == 1 && ("FundsDeposited".equals(eventType) || "FundsWithdrawn".equals(eventType))) {
                Map<String, Object> upgraded = new LinkedHashMap<>();
                upgraded.put("amount_cents", ((Number) payload.get("amount_cents")).intValue());
                upgraded.put("currency", "USD");
                return upgraded;
            }
            throw new IllegalArgumentException("missing upcaster for " + eventType + " v" + fromVersion);
        }
    }

    public static final class AccountProjector {
        private int balanceCents = 0;

        public void apply(CanonicalEvent event) {
            if (!"USD".equals(event.getCurrency())) {
                throw new IllegalArgumentException("unsupported currency: " + event.getCurrency());
            }
            switch (event.getEventType()) {
                case "FundsDeposited":
                    balanceCents += event.getAmountCents();
                    return;
                case "FundsWithdrawn":
                    balanceCents -= event.getAmountCents();
                    return;
                default:
                    throw new IllegalArgumentException("unsupported eventType: " + event.getEventType());
            }
        }

        public int getBalanceCents() {
            return balanceCents;
        }
    }

    public static final class CommandService {
        private static final int LATEST_VERSION = 2;
        private final EventStore store;

        public CommandService(EventStore store) {
            this.store = store;
        }

        public void deposit(String accountId, int amountCents, String currency) {
            if (amountCents <= 0) {
                throw new IllegalArgumentException("amountCents must be positive");
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("amount_cents", amountCents);
            payload.put("currency", currency);
            store.append(accountId, "FundsDeposited", LATEST_VERSION, payload);
        }

        public void withdraw(String accountId, int amountCents, String currency) {
            if (amountCents <= 0) {
                throw new IllegalArgumentException("amountCents must be positive");
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("amount_cents", amountCents);
            payload.put("currency", currency);
            store.append(accountId, "FundsWithdrawn", LATEST_VERSION, payload);
        }
    }

    public static int rebuildBalance(
        String accountId,
        EventStore store,
        EventUpcaster upcaster
    ) {
        AccountProjector projector = new AccountProjector();
        for (RawEvent rawEvent : store.eventsForAccount(accountId)) {
            CanonicalEvent canonical = upcaster.upcast(rawEvent);
            projector.apply(canonical);
        }
        return projector.getBalanceCents();
    }
}
