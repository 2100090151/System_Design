from __future__ import annotations

from dataclasses import dataclass
from enum import Enum


class ResultStatus(Enum):
    SUCCESS = "SUCCESS"
    FAILED = "FAILED"


@dataclass(frozen=True)
class ReplicaEndpoint:
    name: str
    latency_ms: int
    will_succeed: bool = True


@dataclass(frozen=True)
class AttemptRecord:
    endpoint_name: str
    start_ms: int
    finish_ms: int
    will_succeed: bool
    sequence: int


@dataclass(frozen=True)
class ExecutionResult:
    status: ResultStatus
    winner: str | None
    completion_ms: int
    duplicates_issued: int
    tail_saved_ms: int
    timeline: list[str]


class HedgedRequestExecutor:
    def execute(self, replicas: list[ReplicaEndpoint], hedge_delay_ms: int) -> ExecutionResult:
        if hedge_delay_ms <= 0:
            raise ValueError("hedge_delay_ms must be positive")
        if len(replicas) < 2:
            raise ValueError("replicas must contain at least two endpoints")
        if any(replica.latency_ms <= 0 for replica in replicas):
            raise ValueError("replica latency_ms must be positive")

        attempts: list[AttemptRecord] = []
        for sequence, replica in enumerate(replicas):
            start_ms = 0 if sequence == 0 else hedge_delay_ms * sequence
            attempts.append(
                AttemptRecord(
                    endpoint_name=replica.name,
                    start_ms=start_ms,
                    finish_ms=start_ms + replica.latency_ms,
                    will_succeed=replica.will_succeed,
                    sequence=sequence,
                )
            )

        successful_attempts = [attempt for attempt in attempts if attempt.will_succeed]
        winner_attempt: AttemptRecord | None = None
        winner_time: int | None = None
        if successful_attempts:
            winner_attempt = min(successful_attempts, key=lambda attempt: (attempt.finish_ms, attempt.sequence))
            winner_time = winner_attempt.finish_ms

        if winner_time is None:
            launched_attempts = attempts
        else:
            launched_attempts = [
                attempt
                for attempt in attempts
                if attempt is winner_attempt or attempt.start_ms < winner_time
            ]

        launched_keys = {
            (attempt.endpoint_name, attempt.sequence, attempt.start_ms, attempt.finish_ms)
            for attempt in launched_attempts
        }
        duplicates_issued = max(0, len(launched_attempts) - 1)
        primary_attempt = attempts[0]
        completion_ms = (
            winner_time
            if winner_time is not None
            else max(attempt.finish_ms for attempt in launched_attempts)
        )
        tail_saved_ms = (
            max(0, primary_attempt.finish_ms - winner_time)
            if winner_time is not None
            else 0
        )

        timeline = [f"request started hedge_delay={hedge_delay_ms}ms"]
        for attempt in attempts:
            key = (attempt.endpoint_name, attempt.sequence, attempt.start_ms, attempt.finish_ms)
            if key in launched_keys:
                timeline.append(
                    f"{attempt.endpoint_name}: launched at t={attempt.start_ms}ms "
                    f"expected_finish={attempt.finish_ms}ms"
                )
            else:
                timeline.append(
                    f"{attempt.endpoint_name}: not launched "
                    f"(winner decided at {winner_time}ms)"
                )

        for attempt in sorted(launched_attempts, key=lambda value: (value.finish_ms, value.sequence)):
            if winner_attempt is not None and attempt == winner_attempt:
                timeline.append(
                    f"{attempt.endpoint_name}: winner success at t={attempt.finish_ms}ms"
                )
            elif winner_time is not None and attempt.finish_ms > winner_time:
                timeline.append(
                    f"{attempt.endpoint_name}: cancelled at winner_time={winner_time}ms "
                    f"before finish={attempt.finish_ms}ms"
                )
            elif attempt.will_succeed:
                timeline.append(
                    f"{attempt.endpoint_name}: success at t={attempt.finish_ms}ms (lost race)"
                )
            else:
                timeline.append(f"{attempt.endpoint_name}: failed at t={attempt.finish_ms}ms")

        status = ResultStatus.SUCCESS if winner_attempt is not None else ResultStatus.FAILED
        timeline.append(
            f"request finished status={status.value} completion={completion_ms}ms "
            f"duplicates_issued={duplicates_issued} tail_saved={tail_saved_ms}ms"
        )
        return ExecutionResult(
            status=status,
            winner=winner_attempt.endpoint_name if winner_attempt else None,
            completion_ms=completion_ms,
            duplicates_issued=duplicates_issued,
            tail_saved_ms=tail_saved_ms,
            timeline=timeline,
        )
