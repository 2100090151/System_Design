from __future__ import annotations

import hashlib
from dataclasses import dataclass


@dataclass(frozen=True)
class ProofStep:
    sibling_hash: str
    sibling_on_left: bool


class MerkleTree:
    def __init__(self, values: list[str]) -> None:
        if not values:
            raise ValueError("values must be non-empty")
        if any(not value for value in values):
            raise ValueError("values must not contain empty items")

        self._values = values[:]
        self._levels = self._build_levels(values)

    @property
    def root_hash(self) -> str:
        return self._levels[-1][0]

    def proof_for(self, index: int) -> list[ProofStep]:
        if index < 0 or index >= len(self._values):
            raise IndexError("index out of range")

        proof: list[ProofStep] = []
        current_index = index

        for level in self._levels[:-1]:
            sibling_index = current_index - 1 if current_index % 2 else current_index + 1
            if sibling_index >= len(level):
                sibling_index = current_index

            proof.append(
                ProofStep(
                    sibling_hash=level[sibling_index],
                    sibling_on_left=sibling_index < current_index,
                )
            )
            current_index //= 2

        return proof

    @staticmethod
    def verify_proof(value: str, proof: list[ProofStep], expected_root: str) -> bool:
        if not value:
            raise ValueError("value must be non-empty")

        current_hash = MerkleTree._leaf_hash(value)
        for step in proof:
            if step.sibling_on_left:
                current_hash = MerkleTree._parent_hash(step.sibling_hash, current_hash)
            else:
                current_hash = MerkleTree._parent_hash(current_hash, step.sibling_hash)
        return current_hash == expected_root

    @classmethod
    def _build_levels(cls, values: list[str]) -> list[list[str]]:
        levels: list[list[str]] = [[cls._leaf_hash(value) for value in values]]

        while len(levels[-1]) > 1:
            current = levels[-1]
            next_level: list[str] = []
            for index in range(0, len(current), 2):
                left = current[index]
                right = current[index + 1] if index + 1 < len(current) else current[index]
                next_level.append(cls._parent_hash(left, right))
            levels.append(next_level)

        return levels

    @staticmethod
    def _leaf_hash(value: str) -> str:
        return hashlib.sha256(f"leaf:{value}".encode("utf-8")).hexdigest()

    @staticmethod
    def _parent_hash(left: str, right: str) -> str:
        return hashlib.sha256(f"node:{left}:{right}".encode("utf-8")).hexdigest()
