from partitioned_log import Message, PartitionedLog


def print_batch(label: str, batch: list[Message[str]]) -> None:
    print(label)
    if not batch:
        print("  (empty)")
        return

    for message in batch:
        print(
            "  "
            + f"p{message.partition}@{message.offset} "
            + f"key={message.key} payload={message.payload}"
        )


def main() -> None:
    log = PartitionedLog[str](topic="orders", partitions=3)

    events = [
        ("user-1", "order-1001-created"),
        ("user-2", "order-2001-created"),
        ("user-1", "order-1001-paid"),
        ("user-3", "order-3001-created"),
        ("user-2", "order-2001-cancelled"),
    ]

    for key, payload in events:
        message = log.produce(key=key, payload=payload)
        print(
            "produce -> "
            + f"p{message.partition}@{message.offset} "
            + f"key={message.key} payload={message.payload}"
        )

    log.register_group("billing")
    log.register_group("analytics")

    print("billing lag before poll:", log.lag("billing"))

    billing_first = log.poll("billing", max_messages=4)
    print_batch("billing poll #1 (before commit)", billing_first)
    print("billing committed offsets:", log.committed_offsets("billing"))

    log.commit("billing", billing_first[:2])
    print("billing committed after partial commit:", log.committed_offsets("billing"))
    print("billing lag after partial commit:", log.lag("billing"))

    billing_second = log.poll("billing", max_messages=4)
    print_batch("billing poll #2 (uncommitted messages replay)", billing_second)

    log.commit("billing", billing_second)
    print("billing committed after full commit:", log.committed_offsets("billing"))
    print("billing lag final:", log.lag("billing"))

    analytics_first = log.poll("analytics", max_messages=3)
    print_batch("analytics poll #1 (independent group)", analytics_first)
    print("analytics committed offsets:", log.committed_offsets("analytics"))


if __name__ == "__main__":
    main()
