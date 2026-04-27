from __future__ import annotations

from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class RawEvent:
    sequence: int
    account_id: str
    event_type: str
    schema_version: int
    payload: dict[str, Any]


@dataclass(frozen=True)
class CanonicalEvent:
    sequence: int
    account_id: str
    event_type: str
    schema_version: int
    amount_cents: int
    currency: str


class EventStore:
    def __init__(self) -> None:
        self._events: list[RawEvent] = []
        self._next_sequence = 1

    def append(
        self,
        account_id: str,
        event_type: str,
        schema_version: int,
        payload: dict[str, Any],
    ) -> RawEvent:
        if not account_id:
            raise ValueError("account_id must be non-empty")
        if not event_type:
            raise ValueError("event_type must be non-empty")
        if schema_version <= 0:
            raise ValueError("schema_version must be positive")

        event = RawEvent(
            sequence=self._next_sequence,
            account_id=account_id,
            event_type=event_type,
            schema_version=schema_version,
            payload=payload,
        )
        self._events.append(event)
        self._next_sequence += 1
        return event

    def events_for_account(self, account_id: str) -> list[RawEvent]:
        return [event for event in self._events if event.account_id == account_id]


class EventUpcaster:
    _LATEST_VERSION_BY_TYPE = {
        "FundsDeposited": 2,
        "FundsWithdrawn": 2,
    }

    def upcast(self, event: RawEvent) -> CanonicalEvent:
        latest = self._LATEST_VERSION_BY_TYPE.get(event.event_type)
        if latest is None:
            raise ValueError(f"unsupported event_type: {event.event_type}")
        if event.schema_version > latest:
            raise ValueError(
                f"unsupported future schema for {event.event_type}: {event.schema_version} > {latest}"
            )

        current_version = event.schema_version
        payload = dict(event.payload)

        while current_version < latest:
            payload = self._upcast_once(event.event_type, current_version, payload)
            current_version += 1

        amount_cents = int(payload["amount_cents"])
        currency = str(payload["currency"])
        return CanonicalEvent(
            sequence=event.sequence,
            account_id=event.account_id,
            event_type=event.event_type,
            schema_version=current_version,
            amount_cents=amount_cents,
            currency=currency,
        )

    @staticmethod
    def _upcast_once(
        event_type: str, from_version: int, payload: dict[str, Any]
    ) -> dict[str, Any]:
        if from_version == 1 and event_type in {"FundsDeposited", "FundsWithdrawn"}:
            return {
                "amount_cents": int(payload["amount_cents"]),
                "currency": "USD",
            }
        raise ValueError(f"missing upcaster for {event_type} v{from_version}")


class AccountProjector:
    def __init__(self) -> None:
        self.balance_cents = 0

    def apply(self, event: CanonicalEvent) -> None:
        if event.currency != "USD":
            raise ValueError(f"unsupported currency: {event.currency}")

        if event.event_type == "FundsDeposited":
            self.balance_cents += event.amount_cents
            return
        if event.event_type == "FundsWithdrawn":
            self.balance_cents -= event.amount_cents
            return
        raise ValueError(f"unsupported event_type: {event.event_type}")


class CommandService:
    _LATEST_VERSION = 2

    def __init__(self, event_store: EventStore) -> None:
        self._event_store = event_store

    def deposit(self, account_id: str, amount_cents: int, currency: str = "USD") -> None:
        if amount_cents <= 0:
            raise ValueError("amount_cents must be positive")
        self._event_store.append(
            account_id=account_id,
            event_type="FundsDeposited",
            schema_version=self._LATEST_VERSION,
            payload={"amount_cents": amount_cents, "currency": currency},
        )

    def withdraw(self, account_id: str, amount_cents: int, currency: str = "USD") -> None:
        if amount_cents <= 0:
            raise ValueError("amount_cents must be positive")
        self._event_store.append(
            account_id=account_id,
            event_type="FundsWithdrawn",
            schema_version=self._LATEST_VERSION,
            payload={"amount_cents": amount_cents, "currency": currency},
        )


def rebuild_balance(account_id: str, store: EventStore, upcaster: EventUpcaster) -> int:
    projector = AccountProjector()
    for raw_event in store.events_for_account(account_id):
        canonical = upcaster.upcast(raw_event)
        projector.apply(canonical)
    return projector.balance_cents
