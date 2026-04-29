from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class Message:
    message_id: str
    account_id: str
    amount_cents: int


class DedupStore:
    def __init__(self) -> None:
        self._seen: set[str] = set()

    def has_seen(self, message_id: str) -> bool:
        return message_id in self._seen

    def mark_seen(self, message_id: str) -> None:
        self._seen.add(message_id)


class BalanceLedger:
    def __init__(self) -> None:
        self._balances: dict[str, int] = {}

    def apply(self, account_id: str, amount_cents: int) -> None:
        self._balances[account_id] = self._balances.get(account_id, 0) + amount_cents

    def get_balance(self, account_id: str) -> int:
        return self._balances.get(account_id, 0)


class PaymentConsumer:
    def __init__(self, dedup_store: DedupStore, ledger: BalanceLedger) -> None:
        self._dedup = dedup_store
        self._ledger = ledger
        self.applied_count = 0
        self.duplicate_count = 0

    def handle(self, message: Message) -> str:
        if self._dedup.has_seen(message.message_id):
            self.duplicate_count += 1
            return f"skipped duplicate {message.message_id}"

        self._ledger.apply(message.account_id, message.amount_cents)
        self._dedup.mark_seen(message.message_id)
        self.applied_count += 1
        return f"applied {message.message_id} amount={message.amount_cents}"

    def balance(self, account_id: str) -> int:
        return self._ledger.get_balance(account_id)
