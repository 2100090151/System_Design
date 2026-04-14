from __future__ import annotations

from collections import OrderedDict
from dataclasses import dataclass, field
from typing import Generic, TypeVar

K = TypeVar("K")
V = TypeVar("V")


@dataclass
class LRUCache(Generic[K, V]):
    capacity: int
    _items: OrderedDict[K, V] = field(default_factory=OrderedDict, init=False)

    def __post_init__(self) -> None:
        if self.capacity <= 0:
            raise ValueError("capacity must be positive")

    def get(self, key: K) -> V | None:
        if key not in self._items:
            return None

        self._items.move_to_end(key)
        return self._items[key]

    def put(self, key: K, value: V) -> str | None:
        evicted: str | None = None
        if key in self._items:
            self._items.move_to_end(key)
        self._items[key] = value

        if len(self._items) > self.capacity:
            evicted_key, _ = self._items.popitem(last=False)
            evicted = str(evicted_key)
        return evicted

    def snapshot(self) -> list[tuple[K, V]]:
        return list(self._items.items())
