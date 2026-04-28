from schema_registry import ProducerSimulator, SchemaField, SchemaRegistry


def main() -> None:
    registry = SchemaRegistry()
    subject = "order-created"

    v1 = registry.register(
        subject,
        [
            SchemaField("order_id", "string", True),
            SchemaField("amount_cents", "int", True),
        ],
    )
    print(f"registered {subject} schema v{v1.version}")

    v2 = registry.register(
        subject,
        [
            SchemaField("order_id", "string", True),
            SchemaField("amount_cents", "int", True),
            SchemaField("currency", "string", False),
        ],
    )
    print(f"registered {subject} schema v{v2.version} (backward compatible)")

    producer = ProducerSimulator(registry, subject)
    print(
        producer.publish(
            2,
            {"order_id": "ord-1001", "amount_cents": 1599, "currency": "USD"},
        )
    )
    print(
        producer.publish(
            1,
            {"order_id": "ord-1002", "amount_cents": 2099},
        )
    )

    try:
        registry.register(
            subject,
            [
                SchemaField("order_id", "string", True),
                # incompatible: removed amount_cents
            ],
        )
    except ValueError as err:
        print(f"incompatible schema rejected: {err}")

    try:
        producer.publish(2, {"order_id": "ord-1003"})
    except ValueError as err:
        print(f"invalid message rejected: {err}")


if __name__ == "__main__":
    main()
