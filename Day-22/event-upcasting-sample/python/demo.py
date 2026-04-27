from event_upcasting_ledger import (
    CommandService,
    EventStore,
    EventUpcaster,
    rebuild_balance,
)


def main() -> None:
    store = EventStore()
    upcaster = EventUpcaster()
    commands = CommandService(store)
    account_id = "acct-2200"

    # Simulate a legacy service that produced v1 events without currency.
    store.append(
        account_id=account_id,
        event_type="FundsDeposited",
        schema_version=1,
        payload={"amount_cents": 10_000},
    )
    store.append(
        account_id=account_id,
        event_type="FundsWithdrawn",
        schema_version=1,
        payload={"amount_cents": 2_000},
    )

    # Current service writes latest v2 schema.
    commands.deposit(account_id, 1_500, currency="USD")
    commands.withdraw(account_id, 700, currency="USD")

    print("raw events")
    for event in store.events_for_account(account_id):
        print(
            f"  seq={event.sequence} type={event.event_type} "
            f"v={event.schema_version} payload={event.payload}"
        )

    print("\ncanonical events after upcasting")
    for event in store.events_for_account(account_id):
        canonical = upcaster.upcast(event)
        print(
            f"  seq={canonical.sequence} type={canonical.event_type} "
            f"v={canonical.schema_version} amount={canonical.amount_cents} "
            f"currency={canonical.currency}"
        )

    balance = rebuild_balance(account_id, store, upcaster)
    print(f"\nrebuilt balance: {balance}")


if __name__ == "__main__":
    main()
