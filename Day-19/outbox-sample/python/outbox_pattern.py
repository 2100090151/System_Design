from __future__ import annotations

from dataclasses import dataclass
from enum import Enum


class OutboxStatus(Enum):
    PENDING = "PENDING"
    SENT = "SENT"


@dataclass(frozen=True)
class Order:
    order_id: str
    amount_cents: int


@dataclass
class OutboxMessage:
    message_id: int
    aggregate_id: str
    topic: str
    payload: str
    status: OutboxStatus = OutboxStatus.PENDING
    attempts: int = 0


class InMemoryStore:
    def __init__(self) -> None:
        self._orders: dict[str, Order] = {}
        self._outbox: list[OutboxMessage] = []
        self._next_message_id = 1

    def begin_transaction(self) -> Transaction:
        return Transaction(self)

    def _allocate_message_id(self) -> int:
        message_id = self._next_message_id
        self._next_message_id += 1
        return message_id

    def pending_messages(self, limit: int) -> list[OutboxMessage]:
        if limit <= 0:
            return []
        pending = [m for m in self._outbox if m.status == OutboxStatus.PENDING]
        return pending[:limit]

    def mark_sent(self, message_id: int) -> None:
        message = self._find_message(message_id)
        message.status = OutboxStatus.SENT

    def mark_failure(self, message_id: int) -> None:
        message = self._find_message(message_id)
        message.attempts += 1

    def _find_message(self, message_id: int) -> OutboxMessage:
        for message in self._outbox:
            if message.message_id == message_id:
                return message
        raise KeyError(f"unknown message_id: {message_id}")

    def orders_snapshot(self) -> dict[str, Order]:
        return dict(self._orders)

    def outbox_snapshot(self) -> list[OutboxMessage]:
        return [
            OutboxMessage(
                message_id=m.message_id,
                aggregate_id=m.aggregate_id,
                topic=m.topic,
                payload=m.payload,
                status=m.status,
                attempts=m.attempts,
            )
            for m in self._outbox
        ]


class Transaction:
    def __init__(self, store: InMemoryStore) -> None:
        self._store = store
        self._staged_order: Order | None = None
        self._staged_event: OutboxMessage | None = None

    def create_order(self, order_id: str, amount_cents: int) -> None:
        if order_id in self._store._orders:
            raise ValueError(f"order already exists: {order_id}")
        if amount_cents <= 0:
            raise ValueError("amount_cents must be positive")

        self._staged_order = Order(order_id=order_id, amount_cents=amount_cents)
        self._staged_event = OutboxMessage(
            message_id=self._store._allocate_message_id(),
            aggregate_id=order_id,
            topic="orders.created",
            payload=f"{{'order_id':'{order_id}','amount_cents':{amount_cents}}}",
        )

    def commit(self, simulate_db_failure: bool = False) -> None:
        if self._staged_order is None or self._staged_event is None:
            raise ValueError("transaction has no staged work")
        if simulate_db_failure:
            raise RuntimeError("simulated database failure before commit")

        self._store._orders[self._staged_order.order_id] = self._staged_order
        self._store._outbox.append(self._staged_event)


class UnreliableBroker:
    def __init__(self, fail_on_calls: set[int] | None = None) -> None:
        self._fail_on_calls = fail_on_calls or set()
        self._call_count = 0
        self.published: list[str] = []

    def publish(self, topic: str, payload: str) -> bool:
        self._call_count += 1
        if self._call_count in self._fail_on_calls:
            return False
        self.published.append(f"{topic}:{payload}")
        return True


class OutboxRelay:
    def __init__(self, store: InMemoryStore, broker: UnreliableBroker) -> None:
        self._store = store
        self._broker = broker

    def poll_and_publish(self, limit: int = 10) -> list[str]:
        history: list[str] = []
        for message in self._store.pending_messages(limit):
            ok = self._broker.publish(message.topic, message.payload)
            if ok:
                self._store.mark_sent(message.message_id)
                history.append(f"published message {message.message_id}")
            else:
                self._store.mark_failure(message.message_id)
                history.append(
                    f"publish failed for message {message.message_id}; will retry"
                )
        return history
