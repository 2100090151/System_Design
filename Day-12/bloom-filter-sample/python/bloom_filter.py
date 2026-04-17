from __future__ import annotations

import hashlib
import math


class BloomFilter:
    def __init__(self, size_bits: int, hash_count: int) -> None:
        if size_bits <= 0:
            raise ValueError("size_bits must be positive")
        if hash_count <= 0:
            raise ValueError("hash_count must be positive")

        self.size_bits = size_bits
        self.hash_count = hash_count
        self._bits = bytearray((size_bits + 7) // 8)
        self._insertions = 0

    def add(self, item: str) -> None:
        for index in self._indexes(item):
            self._set_bit(index)
        self._insertions += 1

    def might_contain(self, item: str) -> bool:
        return all(self._get_bit(index) for index in self._indexes(item))

    def fill_ratio(self) -> float:
        bits_set = sum(byte.bit_count() for byte in self._bits)
        return bits_set / self.size_bits

    def estimated_false_positive_rate(self) -> float:
        m = self.size_bits
        k = self.hash_count
        n = self._insertions
        return (1 - math.exp((-k * n) / m)) ** k

    def _indexes(self, item: str) -> list[int]:
        if not item:
            raise ValueError("item must be non-empty")

        return [self._index_for_seed(item, seed) for seed in range(self.hash_count)]

    def _index_for_seed(self, item: str, seed: int) -> int:
        digest = hashlib.sha256(f"{seed}:{item}".encode("utf-8")).digest()
        return int.from_bytes(digest[:8], byteorder="big") % self.size_bits

    def _set_bit(self, index: int) -> None:
        byte_index, bit_offset = divmod(index, 8)
        self._bits[byte_index] |= 1 << bit_offset

    def _get_bit(self, index: int) -> bool:
        byte_index, bit_offset = divmod(index, 8)
        return bool(self._bits[byte_index] & (1 << bit_offset))
