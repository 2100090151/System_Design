import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EventSourcingAccount {
    public static final class DomainEvent {
        private final int sequence;
        private final String accountId;
        private final String eventType;
        private final int amountCents;
        private final int version;

        public DomainEvent(int sequence, String accountId, String eventType, int amountCents, int version) {
            this.sequence = sequence;
            this.accountId = accountId;
            this.eventType = eventType;
            this.amountCents = amountCents;
            this.version = version;
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

        public int getAmountCents() {
            return amountCents;
        }

        public int getVersion() {
            return version;
        }
    }

    public static final class AccountSnapshot {
        private final String accountId;
        private final int balanceCents;
        private final int version;
        private final int lastSequence;

        public AccountSnapshot(String accountId, int balanceCents, int version, int lastSequence) {
            this.accountId = accountId;
            this.balanceCents = balanceCents;
            this.version = version;
            this.lastSequence = lastSequence;
        }

        public String getAccountId() {
            return accountId;
        }

        public int getBalanceCents() {
            return balanceCents;
        }

        public int getVersion() {
            return version;
        }

        public int getLastSequence() {
            return lastSequence;
        }
    }

    public static final class EventStore {
        private final List<DomainEvent> events = new ArrayList<>();
        private int nextSequence = 1;

        public DomainEvent append(
            String accountId,
            String eventType,
            int amountCents,
            int expectedVersion
        ) {
            int currentVersion = latestVersion(accountId);
            if (currentVersion != expectedVersion) {
                throw new IllegalArgumentException(
                    "version mismatch for "
                        + accountId
                        + ": expected "
                        + expectedVersion
                        + ", got "
                        + currentVersion
                );
            }

            DomainEvent event = new DomainEvent(
                nextSequence,
                accountId,
                eventType,
                amountCents,
                currentVersion + 1
            );
            events.add(event);
            nextSequence += 1;
            return event;
        }

        public int latestVersion(String accountId) {
            int version = 0;
            for (DomainEvent event : events) {
                if (accountId.equals(event.getAccountId()) && event.getVersion() > version) {
                    version = event.getVersion();
                }
            }
            return version;
        }

        public List<DomainEvent> eventsForAccount(String accountId, int afterSequence) {
            List<DomainEvent> result = new ArrayList<>();
            for (DomainEvent event : events) {
                if (!accountId.equals(event.getAccountId())) {
                    continue;
                }
                if (event.getSequence() <= afterSequence) {
                    continue;
                }
                result.add(event);
            }
            return result;
        }

        public List<DomainEvent> allEvents() {
            return new ArrayList<>(events);
        }
    }

    public static final class SnapshotStore {
        private final Map<String, AccountSnapshot> snapshots = new LinkedHashMap<>();

        public void put(AccountSnapshot snapshot) {
            snapshots.put(snapshot.getAccountId(), snapshot);
        }

        public AccountSnapshot get(String accountId) {
            return snapshots.get(accountId);
        }
    }

    public static final class AccountAggregate {
        private final String accountId;
        private boolean open;
        private int balanceCents;
        private int version;
        private int lastSequence;

        public AccountAggregate(String accountId) {
            this.accountId = accountId;
            this.open = false;
            this.balanceCents = 0;
            this.version = 0;
            this.lastSequence = 0;
        }

        public void apply(DomainEvent event) {
            switch (event.getEventType()) {
                case "AccountOpened":
                    open = true;
                    break;
                case "FundsDeposited":
                    balanceCents += event.getAmountCents();
                    break;
                case "FundsWithdrawn":
                    balanceCents -= event.getAmountCents();
                    break;
                default:
                    throw new IllegalArgumentException("unknown eventType: " + event.getEventType());
            }
            version = event.getVersion();
            lastSequence = event.getSequence();
        }

        public static AccountAggregate rehydrate(
            String accountId,
            List<DomainEvent> events,
            AccountSnapshot snapshot
        ) {
            AccountAggregate aggregate = new AccountAggregate(accountId);
            if (snapshot != null) {
                aggregate.open = true;
                aggregate.balanceCents = snapshot.getBalanceCents();
                aggregate.version = snapshot.getVersion();
                aggregate.lastSequence = snapshot.getLastSequence();
            }

            for (DomainEvent event : events) {
                aggregate.apply(event);
            }
            return aggregate;
        }

        public String getAccountId() {
            return accountId;
        }

        public boolean isOpen() {
            return open;
        }

        public int getBalanceCents() {
            return balanceCents;
        }

        public int getVersion() {
            return version;
        }

        public int getLastSequence() {
            return lastSequence;
        }
    }

    public static final class CommandService {
        private final EventStore eventStore;
        private final SnapshotStore snapshotStore;

        public CommandService(EventStore eventStore, SnapshotStore snapshotStore) {
            this.eventStore = eventStore;
            this.snapshotStore = snapshotStore;
        }

        private AccountAggregate load(String accountId) {
            AccountSnapshot snapshot = snapshotStore.get(accountId);
            int after = snapshot != null ? snapshot.getLastSequence() : 0;
            List<DomainEvent> tail = eventStore.eventsForAccount(accountId, after);
            return AccountAggregate.rehydrate(accountId, tail, snapshot);
        }

        public DomainEvent openAccount(String accountId) {
            AccountAggregate aggregate = load(accountId);
            if (aggregate.isOpen()) {
                throw new IllegalArgumentException("account already open: " + accountId);
            }
            return eventStore.append(accountId, "AccountOpened", 0, aggregate.getVersion());
        }

        public DomainEvent deposit(String accountId, int amountCents) {
            if (amountCents <= 0) {
                throw new IllegalArgumentException("amountCents must be positive");
            }
            AccountAggregate aggregate = load(accountId);
            if (!aggregate.isOpen()) {
                throw new IllegalArgumentException("account not open: " + accountId);
            }
            return eventStore.append(
                accountId,
                "FundsDeposited",
                amountCents,
                aggregate.getVersion()
            );
        }

        public DomainEvent withdraw(String accountId, int amountCents) {
            if (amountCents <= 0) {
                throw new IllegalArgumentException("amountCents must be positive");
            }
            AccountAggregate aggregate = load(accountId);
            if (!aggregate.isOpen()) {
                throw new IllegalArgumentException("account not open: " + accountId);
            }
            if (aggregate.getBalanceCents() < amountCents) {
                throw new IllegalArgumentException("insufficient balance");
            }
            return eventStore.append(
                accountId,
                "FundsWithdrawn",
                amountCents,
                aggregate.getVersion()
            );
        }

        public AccountSnapshot snapshot(String accountId) {
            AccountAggregate aggregate = load(accountId);
            if (!aggregate.isOpen()) {
                throw new IllegalArgumentException("account not open: " + accountId);
            }
            AccountSnapshot snapshot = new AccountSnapshot(
                accountId,
                aggregate.getBalanceCents(),
                aggregate.getVersion(),
                aggregate.getLastSequence()
            );
            snapshotStore.put(snapshot);
            return snapshot;
        }

        public AccountAggregate rebuildFull(String accountId) {
            return AccountAggregate.rehydrate(accountId, eventStore.eventsForAccount(accountId, 0), null);
        }

        public AccountAggregate rebuildWithSnapshot(String accountId) {
            AccountSnapshot snapshot = snapshotStore.get(accountId);
            int after = snapshot != null ? snapshot.getLastSequence() : 0;
            return AccountAggregate.rehydrate(accountId, eventStore.eventsForAccount(accountId, after), snapshot);
        }
    }
}
