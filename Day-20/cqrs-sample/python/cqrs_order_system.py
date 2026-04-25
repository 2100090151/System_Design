from __future__ import annotations

from dataclasses import dataclass
from enum import Enum


class OrderStatus(Enum):
    CREATED = "CREATED"
    CONFIRMED = "CONFIRMED"


@dataclass(frozen=True)
class OrderAggregate:
    order_id: str
    customer_id: str
    amount_cents: int
    status: OrderStatus


@dataclass(frozen=True)
class DomainEvent:
    sequence: int
    event_type: str
    order_id: str
    customer_id: str
    amount_cents: int


@dataclass
class OrderReadModel:
    order_id: str
    customer_id: str
    amount_cents: int
    status: str


class WriteStore:
    def __init__(self) -> None:
        self.orders: dict[str, OrderAggregate] = {}
        self.events: list[DomainEvent] = []
        self._next_sequence = 1

    def append_event(
        self, event_type: str, order_id: str, customer_id: str, amount_cents: int
    ) -> DomainEvent:
        event = DomainEvent(
            sequence=self._next_sequence,
            event_type=event_type,
            order_id=order_id,
            customer_id=customer_id,
            amount_cents=amount_cents,
        )
        self._next_sequence += 1
        self.events.append(event)
        return event


class ReadStore:
    def __init__(self) -> None:
        self.orders: dict[str, OrderReadModel] = {}


class CommandService:
    def __init__(self, write_store: WriteStore) -> None:
        self._write_store = write_store

    def create_order(self, order_id: str, customer_id: str, amount_cents: int) -> None:
        if order_id in self._write_store.orders:
            raise ValueError(f"order already exists: {order_id}")
        if amount_cents <= 0:
            raise ValueError("amount_cents must be positive")

        aggregate = OrderAggregate(
            order_id=order_id,
            customer_id=customer_id,
            amount_cents=amount_cents,
            status=OrderStatus.CREATED,
        )
        self._write_store.orders[order_id] = aggregate
        self._write_store.append_event(
            event_type="OrderCreated",
            order_id=order_id,
            customer_id=customer_id,
            amount_cents=amount_cents,
        )

    def confirm_order(self, order_id: str) -> None:
        aggregate = self._write_store.orders.get(order_id)
        if aggregate is None:
            raise ValueError(f"order not found: {order_id}")
        if aggregate.status == OrderStatus.CONFIRMED:
            return

        confirmed = OrderAggregate(
            order_id=aggregate.order_id,
            customer_id=aggregate.customer_id,
            amount_cents=aggregate.amount_cents,
            status=OrderStatus.CONFIRMED,
        )
        self._write_store.orders[order_id] = confirmed
        self._write_store.append_event(
            event_type="OrderConfirmed",
            order_id=order_id,
            customer_id=aggregate.customer_id,
            amount_cents=aggregate.amount_cents,
        )


class Projector:
    def __init__(self, write_store: WriteStore, read_store: ReadStore) -> None:
        self._write_store = write_store
        self._read_store = read_store
        self._last_sequence = 0

    @property
    def last_sequence(self) -> int:
        return self._last_sequence

    def project_next(self, batch_size: int = 10) -> list[str]:
        history: list[str] = []
        if batch_size <= 0:
            return history

        unapplied = [e for e in self._write_store.events if e.sequence > self._last_sequence]
        for event in unapplied[:batch_size]:
            if event.event_type == "OrderCreated":
                self._read_store.orders[event.order_id] = OrderReadModel(
                    order_id=event.order_id,
                    customer_id=event.customer_id,
                    amount_cents=event.amount_cents,
                    status=OrderStatus.CREATED.value,
                )
                history.append(f"projected {event.event_type}#{event.sequence}")
            elif event.event_type == "OrderConfirmed":
                row = self._read_store.orders.get(event.order_id)
                if row is not None:
                    row.status = OrderStatus.CONFIRMED.value
                history.append(f"projected {event.event_type}#{event.sequence}")
            else:
                history.append(f"skipped unknown event#{event.sequence}")
            self._last_sequence = event.sequence

        return history


class QueryService:
    def __init__(self, read_store: ReadStore) -> None:
        self._read_store = read_store

    def get_order(self, order_id: str) -> OrderReadModel | None:
        row = self._read_store.orders.get(order_id)
        if row is None:
            return None
        return OrderReadModel(
            order_id=row.order_id,
            customer_id=row.customer_id,
            amount_cents=row.amount_cents,
            status=row.status,
        )

    def list_orders_for_customer(self, customer_id: str) -> list[OrderReadModel]:
        rows = [r for r in self._read_store.orders.values() if r.customer_id == customer_id]
        return [
            OrderReadModel(
                order_id=r.order_id,
                customer_id=r.customer_id,
                amount_cents=r.amount_cents,
                status=r.status,
            )
            for r in rows
        ]
