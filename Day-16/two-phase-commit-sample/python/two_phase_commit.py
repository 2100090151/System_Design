from __future__ import annotations

from dataclasses import dataclass
from enum import Enum


class Decision(Enum):
    COMMIT = "COMMIT"
    ABORT = "ABORT"


@dataclass(frozen=True)
class CommittedOperation:
    tx_id: str
    operation: str


class Participant:
    def __init__(self, name: str) -> None:
        self.name = name
        self.available = True
        self._reject_tx_ids: set[str] = set()
        self._pending: dict[str, str] = {}
        self._committed: list[CommittedOperation] = []

    def set_available(self, available: bool) -> None:
        self.available = available

    def set_reject_for_tx(self, tx_id: str, reject: bool) -> None:
        if reject:
            self._reject_tx_ids.add(tx_id)
        else:
            self._reject_tx_ids.discard(tx_id)

    def prepare(self, tx_id: str, operation: str) -> bool:
        if not self.available:
            return False
        if tx_id in self._reject_tx_ids:
            return False
        self._pending[tx_id] = operation
        return True

    def finalize(self, tx_id: str, decision: Decision) -> None:
        if not self.available:
            return
        operation = self._pending.pop(tx_id, None)
        if operation is None:
            return
        if decision == Decision.COMMIT:
            self._committed.append(CommittedOperation(tx_id=tx_id, operation=operation))

    def has_pending(self, tx_id: str) -> bool:
        return tx_id in self._pending

    def has_committed(self, tx_id: str) -> bool:
        return any(item.tx_id == tx_id for item in self._committed)


class TwoPhaseCommitCoordinator:
    def __init__(self, participant_names: list[str]) -> None:
        if not participant_names:
            raise ValueError("participant_names must be non-empty")
        if len(set(participant_names)) != len(participant_names):
            raise ValueError("participant names must be unique")

        self._participants = {name: Participant(name) for name in participant_names}
        self._decision_log: dict[str, Decision] = {}

    def participant(self, name: str) -> Participant:
        participant = self._participants.get(name)
        if participant is None:
            raise ValueError(f"unknown participant: {name}")
        return participant

    def execute(
        self,
        tx_id: str,
        operations_by_participant: dict[str, str],
        drop_before_decision: list[str] | None = None,
    ) -> Decision:
        if not tx_id:
            raise ValueError("tx_id must be non-empty")
        if not operations_by_participant:
            raise ValueError("operations_by_participant must be non-empty")

        prepared_participants: list[str] = []
        all_yes = True

        for participant_name, operation in operations_by_participant.items():
            participant = self.participant(participant_name)
            voted_yes = participant.prepare(tx_id, operation)
            if voted_yes:
                prepared_participants.append(participant_name)
            else:
                all_yes = False

        decision = Decision.COMMIT if all_yes else Decision.ABORT
        self._decision_log[tx_id] = decision

        for participant_name in drop_before_decision or []:
            self.participant(participant_name).set_available(False)

        for participant_name in prepared_participants:
            self.participant(participant_name).finalize(tx_id, decision)

        return decision

    def reconcile_participant(self, participant_name: str, tx_id: str) -> None:
        decision = self._decision_log.get(tx_id)
        if decision is None:
            raise ValueError(f"no decision found for tx_id={tx_id}")
        self.participant(participant_name).finalize(tx_id, decision)

    def decision_for(self, tx_id: str) -> Decision | None:
        return self._decision_log.get(tx_id)

    def states_for_tx(self, tx_id: str) -> list[str]:
        lines: list[str] = []
        for name, participant in self._participants.items():
            lines.append(
                f"{name}: available={participant.available}, "
                f"pending={participant.has_pending(tx_id)}, "
                f"committed={participant.has_committed(tx_id)}"
            )
        return lines
