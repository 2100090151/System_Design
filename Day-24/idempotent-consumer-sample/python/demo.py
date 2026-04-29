from idempotent_consumer import BalanceLedger, DedupStore, Message, PaymentConsumer


def main() -> None:
    dedup = DedupStore()
    ledger = BalanceLedger()
    consumer = PaymentConsumer(dedup, ledger)

    messages = [
        Message("msg-1", "acct-1", 1500),
        Message("msg-2", "acct-1", 2500),
        Message("msg-1", "acct-1", 1500),  # duplicate
        Message("msg-3", "acct-1", -700),
        Message("msg-2", "acct-1", 2500),  # duplicate
        Message("msg-4", "acct-2", 900),
    ]

    for message in messages:
        print(consumer.handle(message))

    print("acct-1 balance:", consumer.balance("acct-1"))
    print("acct-2 balance:", consumer.balance("acct-2"))
    print("applied_count:", consumer.applied_count)
    print("duplicate_count:", consumer.duplicate_count)


if __name__ == "__main__":
    main()
