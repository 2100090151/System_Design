public class Main {
    private static void printAggregate(String prefix, EventSourcingAccount.AccountAggregate aggregate) {
        System.out.println(
            prefix
                + " account="
                + aggregate.getAccountId()
                + " balance="
                + aggregate.getBalanceCents()
                + " version="
                + aggregate.getVersion()
                + " seq="
                + aggregate.getLastSequence()
        );
    }

    public static void main(String[] args) {
        EventSourcingAccount.EventStore events = new EventSourcingAccount.EventStore();
        EventSourcingAccount.SnapshotStore snapshots = new EventSourcingAccount.SnapshotStore();
        EventSourcingAccount.CommandService commands = new EventSourcingAccount.CommandService(events, snapshots);

        String accountId = "acct-100";

        commands.openAccount(accountId);
        commands.deposit(accountId, 10_000);
        commands.deposit(accountId, 2_500);
        commands.withdraw(accountId, 1_200);

        EventSourcingAccount.AccountAggregate beforeSnapshot = commands.rebuildFull(accountId);
        printAggregate("before snapshot:", beforeSnapshot);

        EventSourcingAccount.AccountSnapshot snapshot = commands.snapshot(accountId);
        System.out.println(
            "snapshot created: balance="
                + snapshot.getBalanceCents()
                + " version="
                + snapshot.getVersion()
                + " seq="
                + snapshot.getLastSequence()
        );

        commands.deposit(accountId, 500);
        commands.withdraw(accountId, 300);

        EventSourcingAccount.AccountAggregate full = commands.rebuildFull(accountId);
        EventSourcingAccount.AccountAggregate withSnapshot = commands.rebuildWithSnapshot(accountId);

        printAggregate("full rebuild:", full);
        printAggregate("snapshot rebuild:", withSnapshot);

        System.out.println("event count: " + events.allEvents().size());
        System.out.println("state match: " + (full.getBalanceCents() == withSnapshot.getBalanceCents()));
    }
}
