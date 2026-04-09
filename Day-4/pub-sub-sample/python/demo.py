from event_bus import EventBus


def notification_service(topic: str, payload: dict) -> None:
    print(f"[notification-service] topic={topic} payload={payload}")


def analytics_service(topic: str, payload: dict) -> None:
    print(f"[analytics-service] topic={topic} payload={payload}")


def inventory_service(topic: str, payload: dict) -> None:
    print(f"[inventory-service] topic={topic} payload={payload}")


def main() -> None:
    bus = EventBus()

    bus.subscribe("order.created", notification_service)
    bus.subscribe("order.created", analytics_service)
    bus.subscribe("order.created", inventory_service)
    bus.subscribe("user.signed_up", analytics_service)

    print("Topic snapshot:", bus.topic_snapshot())

    delivered = bus.publish(
        "order.created",
        {"order_id": "ORD-1001", "user_id": "U-42", "amount": 1499},
    )
    print(f"Delivered to {delivered} subscribers")

    delivered = bus.publish(
        "user.signed_up",
        {"user_id": "U-99", "plan": "free"},
    )
    print(f"Delivered to {delivered} subscribers")


if __name__ == "__main__":
    main()
