from __future__ import annotations

from dataclasses import dataclass, field
from threading import Lock
from time import monotonic


@dataclass
class TokenBucketRateLimiter:
    capacity: int
    refill_rate_per_second: float
    _tokens: float = field(init=False)
    _last_refill_time: float = field(init=False)
    _lock: Lock = field(default_factory=Lock, init=False)

    def __post_init__(self) -> None:
        if self.capacity <= 0:
            raise ValueError("capacity must be positive")
        if self.refill_rate_per_second <= 0:
            raise ValueError("refill_rate_per_second must be positive")

        self._tokens = float(self.capacity)
        self._last_refill_time = monotonic()

    def allow_request(self, tokens_required: float = 1.0) -> bool:
        if tokens_required <= 0:
            raise ValueError("tokens_required must be positive")

        with self._lock:
            self._refill()
            if self._tokens >= tokens_required:
                self._tokens -= tokens_required
                return True
            return False

    def available_tokens(self) -> float:
        with self._lock:
            self._refill()
            return self._tokens

    def _refill(self) -> None:
        now = monotonic()
        elapsed = now - self._last_refill_time
        if elapsed <= 0:
            return

        refilled = elapsed * self.refill_rate_per_second
        self._tokens = min(float(self.capacity), self._tokens + refilled)
        self._last_refill_time = now
