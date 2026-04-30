from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class Message:
    message_id: str
    account_id: str
    amount_cents: int


class InboxStore:
    """Durable inbox simulation. In production this would be a DB table with unique message_id."""

    def __init__(self) -> None:
        self._processed: set[str] = set()

    def exists(self, message_id: str) -> bool:
        return message_id in self._processed

    def add(self, message_id: str) -> None:
        self._processed.add(message_id)


class LedgerStore:
    def __init__(self) -> None:
        self._balances: dict[str, int] = {}

    def apply(self, account_id: str, amount_cents: int) -> None:
        self._balances[account_id] = self._balances.get(account_id, 0) + amount_cents

    def balance(self, account_id: str) -> int:
        return self._balances.get(account_id, 0)


class InboxConsumer:
    def __init__(self, inbox: InboxStore, ledger: LedgerStore) -> None:
        self._inbox = inbox
        self._ledger = ledger
        self.applied_count = 0
        self.duplicate_count = 0

    def process(self, message: Message) -> str:
        # Simulate atomic business update + inbox insert.
        if self._inbox.exists(message.message_id):
            self.duplicate_count += 1
            return f"duplicate skipped {message.message_id}"

        self._ledger.apply(message.account_id, message.amount_cents)
        self._inbox.add(message.message_id)
        self.applied_count += 1
        return f"applied {message.message_id} amount={message.amount_cents}"

    def balance(self, account_id: str) -> int:
        return self._ledger.balance(account_id)
