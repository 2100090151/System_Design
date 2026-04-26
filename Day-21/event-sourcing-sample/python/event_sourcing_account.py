from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class DomainEvent:
    sequence: int
    account_id: str
    event_type: str
    amount_cents: int
    version: int


@dataclass(frozen=True)
class AccountSnapshot:
    account_id: str
    balance_cents: int
    version: int
    last_sequence: int


class EventStore:
    def __init__(self) -> None:
        self._events: list[DomainEvent] = []
        self._next_sequence = 1

    def append(
        self,
        account_id: str,
        event_type: str,
        amount_cents: int,
        expected_version: int,
    ) -> DomainEvent:
        current_version = self.latest_version(account_id)
        if current_version != expected_version:
            raise ValueError(
                f"version mismatch for {account_id}: expected {expected_version}, got {current_version}"
            )

        event = DomainEvent(
            sequence=self._next_sequence,
            account_id=account_id,
            event_type=event_type,
            amount_cents=amount_cents,
            version=current_version + 1,
        )
        self._events.append(event)
        self._next_sequence += 1
        return event

    def latest_version(self, account_id: str) -> int:
        versions = [e.version for e in self._events if e.account_id == account_id]
        return max(versions) if versions else 0

    def events_for_account(self, account_id: str, after_sequence: int = 0) -> list[DomainEvent]:
        return [
            e
            for e in self._events
            if e.account_id == account_id and e.sequence > after_sequence
        ]

    def all_events(self) -> list[DomainEvent]:
        return self._events[:]


class SnapshotStore:
    def __init__(self) -> None:
        self._snapshots: dict[str, AccountSnapshot] = {}

    def put(self, snapshot: AccountSnapshot) -> None:
        self._snapshots[snapshot.account_id] = snapshot

    def get(self, account_id: str) -> AccountSnapshot | None:
        return self._snapshots.get(account_id)


class AccountAggregate:
    def __init__(self, account_id: str) -> None:
        self.account_id = account_id
        self.is_open = False
        self.balance_cents = 0
        self.version = 0
        self.last_sequence = 0

    def apply(self, event: DomainEvent) -> None:
        if event.event_type == "AccountOpened":
            self.is_open = True
        elif event.event_type == "FundsDeposited":
            self.balance_cents += event.amount_cents
        elif event.event_type == "FundsWithdrawn":
            self.balance_cents -= event.amount_cents
        else:
            raise ValueError(f"unknown event_type: {event.event_type}")

        self.version = event.version
        self.last_sequence = event.sequence

    @staticmethod
    def rehydrate(
        account_id: str,
        events: list[DomainEvent],
        snapshot: AccountSnapshot | None = None,
    ) -> AccountAggregate:
        agg = AccountAggregate(account_id)
        if snapshot is not None:
            agg.is_open = True
            agg.balance_cents = snapshot.balance_cents
            agg.version = snapshot.version
            agg.last_sequence = snapshot.last_sequence

        for event in events:
            agg.apply(event)
        return agg


class CommandService:
    def __init__(self, event_store: EventStore, snapshot_store: SnapshotStore) -> None:
        self._event_store = event_store
        self._snapshot_store = snapshot_store

    def _load(self, account_id: str) -> AccountAggregate:
        snapshot = self._snapshot_store.get(account_id)
        after = snapshot.last_sequence if snapshot else 0
        tail_events = self._event_store.events_for_account(account_id, after_sequence=after)
        return AccountAggregate.rehydrate(account_id, tail_events, snapshot=snapshot)

    def open_account(self, account_id: str) -> DomainEvent:
        agg = self._load(account_id)
        if agg.is_open:
            raise ValueError(f"account already open: {account_id}")
        return self._event_store.append(
            account_id=account_id,
            event_type="AccountOpened",
            amount_cents=0,
            expected_version=agg.version,
        )

    def deposit(self, account_id: str, amount_cents: int) -> DomainEvent:
        if amount_cents <= 0:
            raise ValueError("amount_cents must be positive")
        agg = self._load(account_id)
        if not agg.is_open:
            raise ValueError(f"account not open: {account_id}")
        return self._event_store.append(
            account_id=account_id,
            event_type="FundsDeposited",
            amount_cents=amount_cents,
            expected_version=agg.version,
        )

    def withdraw(self, account_id: str, amount_cents: int) -> DomainEvent:
        if amount_cents <= 0:
            raise ValueError("amount_cents must be positive")
        agg = self._load(account_id)
        if not agg.is_open:
            raise ValueError(f"account not open: {account_id}")
        if agg.balance_cents < amount_cents:
            raise ValueError("insufficient balance")
        return self._event_store.append(
            account_id=account_id,
            event_type="FundsWithdrawn",
            amount_cents=amount_cents,
            expected_version=agg.version,
        )

    def snapshot(self, account_id: str) -> AccountSnapshot:
        agg = self._load(account_id)
        if not agg.is_open:
            raise ValueError(f"account not open: {account_id}")
        snapshot = AccountSnapshot(
            account_id=account_id,
            balance_cents=agg.balance_cents,
            version=agg.version,
            last_sequence=agg.last_sequence,
        )
        self._snapshot_store.put(snapshot)
        return snapshot

    def rebuild_full(self, account_id: str) -> AccountAggregate:
        events = self._event_store.events_for_account(account_id)
        return AccountAggregate.rehydrate(account_id, events, snapshot=None)

    def rebuild_with_snapshot(self, account_id: str) -> AccountAggregate:
        snapshot = self._snapshot_store.get(account_id)
        after = snapshot.last_sequence if snapshot else 0
        tail = self._event_store.events_for_account(account_id, after_sequence=after)
        return AccountAggregate.rehydrate(account_id, tail, snapshot=snapshot)
