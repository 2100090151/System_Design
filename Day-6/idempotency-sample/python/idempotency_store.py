from __future__ import annotations

from dataclasses import dataclass
from threading import Lock
from typing import Callable, Dict, Generic, TypeVar

T = TypeVar("T")


@dataclass(frozen=True)
class ProcessResult(Generic[T]):
    value: T
    replayed: bool


class IdempotencyStore(Generic[T]):
    def __init__(self) -> None:
        self._responses: Dict[str, T] = {}
        self._lock = Lock()

    def execute(self, key: str, handler: Callable[[], T]) -> ProcessResult[T]:
        if not key:
            raise ValueError("key must be non-empty")

        with self._lock:
            if key in self._responses:
                return ProcessResult(value=self._responses[key], replayed=True)

            value = handler()
            self._responses[key] = value
            return ProcessResult(value=value, replayed=False)

    def processed_keys(self) -> int:
        with self._lock:
            return len(self._responses)
