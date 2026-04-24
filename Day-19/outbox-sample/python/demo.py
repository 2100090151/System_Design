from outbox_pattern import InMemoryStore, OutboxRelay, UnreliableBroker


def print_state(store: InMemoryStore) -> None:
    print("orders:")
    for order_id, order in store.orders_snapshot().items():
        print(f"  {order_id} amount={order.amount_cents}")

    print("outbox:")
    for message in store.outbox_snapshot():
        print(
            "  "
            f"id={message.message_id} aggregate={message.aggregate_id} "
            f"status={message.status.value} attempts={message.attempts}"
        )


def main() -> None:
    store = InMemoryStore()

    tx1 = store.begin_transaction()
    tx1.create_order(order_id="order-100", amount_cents=2599)
    tx1.commit()

    tx2 = store.begin_transaction()
    tx2.create_order(order_id="order-101", amount_cents=1299)
    try:
        tx2.commit(simulate_db_failure=True)
    except RuntimeError as error:
        print("simulated failure:", error)

    print("\nstate after transactions")
    print_state(store)

    broker = UnreliableBroker(fail_on_calls={1})
    relay = OutboxRelay(store=store, broker=broker)

    print("\nrelay pass 1")
    for line in relay.poll_and_publish(limit=10):
        print(" ", line)
    print_state(store)

    print("\nrelay pass 2")
    for line in relay.poll_and_publish(limit=10):
        print(" ", line)
    print_state(store)


if __name__ == "__main__":
    main()
