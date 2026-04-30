from inbox_consumer import InboxConsumer, InboxStore, LedgerStore, Message


def main() -> None:
    inbox = InboxStore()
    ledger = LedgerStore()
    consumer = InboxConsumer(inbox, ledger)

    deliveries = [
        Message("m-100", "acct-A", 1200),
        Message("m-101", "acct-A", 800),
        Message("m-100", "acct-A", 1200),  # duplicate replay
        Message("m-102", "acct-A", -300),
        Message("m-103", "acct-B", 900),
        Message("m-101", "acct-A", 800),   # duplicate replay
    ]

    for message in deliveries:
        print(consumer.process(message))

    print("acct-A balance:", consumer.balance("acct-A"))
    print("acct-B balance:", consumer.balance("acct-B"))
    print("applied_count:", consumer.applied_count)
    print("duplicate_count:", consumer.duplicate_count)


if __name__ == "__main__":
    main()
