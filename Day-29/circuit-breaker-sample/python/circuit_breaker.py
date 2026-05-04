from __future__ import annotations

from dataclasses import dataclass
from enum import Enum


class BreakerState(Enum):
    CLOSED = "CLOSED"
    OPEN = "OPEN"
    HALF_OPEN = "HALF_OPEN"


@dataclass(frozen=True)
class CallOutcome:
    state_before: BreakerState
    allowed: bool
    result: str


class CircuitBreaker:
    def __init__(self, failure_threshold: int, cooldown_ticks: int) -> None:
        if failure_threshold <= 0:
            raise ValueError("failure_threshold must be positive")
        if cooldown_ticks <= 0:
            raise ValueError("cooldown_ticks must be positive")

        self.failure_threshold = failure_threshold
        self.cooldown_ticks = cooldown_ticks
        self.state = BreakerState.CLOSED
        self._consecutive_failures = 0
        self._cooldown_remaining = 0

    def allow(self) -> bool:
        if self.state == BreakerState.OPEN:
            self._cooldown_remaining -= 1
            if self._cooldown_remaining <= 0:
                self.state = BreakerState.HALF_OPEN
                return True
            return False
        return True

    def on_success(self) -> None:
        self._consecutive_failures = 0
        self._cooldown_remaining = 0
        self.state = BreakerState.CLOSED

    def on_failure(self) -> None:
        if self.state == BreakerState.HALF_OPEN:
            self._trip_open()
            return

        self._consecutive_failures += 1
        if self._consecutive_failures >= self.failure_threshold:
            self._trip_open()

    def _trip_open(self) -> None:
        self.state = BreakerState.OPEN
        self._cooldown_remaining = self.cooldown_ticks
        self._consecutive_failures = 0


class DependencySimulator:
    def __init__(self, scripted_results: list[bool]) -> None:
        if not scripted_results:
            raise ValueError("scripted_results must be non-empty")
        self._results = scripted_results[:]
        self._index = 0

    def call(self) -> bool:
        if self._index >= len(self._results):
            return self._results[-1]
        outcome = self._results[self._index]
        self._index += 1
        return outcome


class GuardedClient:
    def __init__(self, breaker: CircuitBreaker, dependency: DependencySimulator) -> None:
        self.breaker = breaker
        self.dependency = dependency
        self.success = 0
        self.failure = 0
        self.blocked = 0

    def request(self) -> CallOutcome:
        before = self.breaker.state
        if not self.breaker.allow():
            self.blocked += 1
            return CallOutcome(state_before=before, allowed=False, result="BLOCKED")

        ok = self.dependency.call()
        if ok:
            self.breaker.on_success()
            self.success += 1
            return CallOutcome(state_before=before, allowed=True, result="SUCCESS")

        self.breaker.on_failure()
        self.failure += 1
        return CallOutcome(state_before=before, allowed=True, result="FAILURE")
