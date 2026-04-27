import java.util.LinkedHashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        EventUpcastingLedger.EventStore store = new EventUpcastingLedger.EventStore();
        EventUpcastingLedger.EventUpcaster upcaster = new EventUpcastingLedger.EventUpcaster();
        EventUpcastingLedger.CommandService commands = new EventUpcastingLedger.CommandService(store);
        String accountId = "acct-2200";

        // Simulate legacy v1 events without currency.
        Map<String, Object> v1Deposit = new LinkedHashMap<>();
        v1Deposit.put("amount_cents", 10_000);
        store.append(accountId, "FundsDeposited", 1, v1Deposit);

        Map<String, Object> v1Withdraw = new LinkedHashMap<>();
        v1Withdraw.put("amount_cents", 2_000);
        store.append(accountId, "FundsWithdrawn", 1, v1Withdraw);

        // Current service writes latest v2 schema.
        commands.deposit(accountId, 1_500, "USD");
        commands.withdraw(accountId, 700, "USD");

        System.out.println("raw events");
        for (EventUpcastingLedger.RawEvent event : store.eventsForAccount(accountId)) {
            System.out.println(
                "  seq="
                    + event.getSequence()
                    + " type="
                    + event.getEventType()
                    + " v="
                    + event.getSchemaVersion()
                    + " payload="
                    + event.getPayload()
            );
        }

        System.out.println("\ncanonical events after upcasting");
        for (EventUpcastingLedger.RawEvent event : store.eventsForAccount(accountId)) {
            EventUpcastingLedger.CanonicalEvent canonical = upcaster.upcast(event);
            System.out.println(
                "  seq="
                    + canonical.getSequence()
                    + " type="
                    + canonical.getEventType()
                    + " v="
                    + canonical.getSchemaVersion()
                    + " amount="
                    + canonical.getAmountCents()
                    + " currency="
                    + canonical.getCurrency()
            );
        }

        int balance = EventUpcastingLedger.rebuildBalance(accountId, store, upcaster);
        System.out.println("\nrebuilt balance: " + balance);
    }
}
