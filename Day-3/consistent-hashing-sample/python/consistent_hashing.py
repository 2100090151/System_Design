from __future__ import annotations

from bisect import bisect_right
from dataclasses import dataclass, field
import hashlib


@dataclass
class ConsistentHashRing:
    virtual_nodes: int = 10
    _ring: list[int] = field(default_factory=list, init=False)
    _owners: dict[int, str] = field(default_factory=dict, init=False)

    def add_node(self, node: str) -> None:
        for replica in range(self.virtual_nodes):
            key = self._hash(f"{node}#{replica}")
            if key not in self._owners:
                self._owners[key] = node
                self._ring.append(key)
        self._ring.sort()

    def get_node(self, item_key: str) -> str:
        if not self._ring:
            raise RuntimeError("Hash ring is empty")

        key = self._hash(item_key)
        index = bisect_right(self._ring, key)
        if index == len(self._ring):
            index = 0
        return self._owners[self._ring[index]]

    def distribution(self, keys: list[str]) -> dict[str, int]:
        result: dict[str, int] = {}
        for key in keys:
            owner = self.get_node(key)
            result[owner] = result.get(owner, 0) + 1
        return dict(sorted(result.items()))

    @staticmethod
    def _hash(value: str) -> int:
        digest = hashlib.sha256(value.encode("utf-8")).hexdigest()
        return int(digest[:8], 16)
