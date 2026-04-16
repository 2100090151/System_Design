from __future__ import annotations

import hashlib
from dataclasses import dataclass
from typing import Dict, Generic, TypeVar

T = TypeVar("T")


@dataclass(frozen=True)
class Message(Generic[T]):
    partition: int
    offset: int
    key: str
    payload: T


class PartitionedLog(Generic[T]):
    def __init__(self, topic: str, partitions: int) -> None:
        if not topic:
            raise ValueError("topic must be non-empty")
        if partitions <= 0:
            raise ValueError("partitions must be positive")

        self.topic = topic
        self._partitions: list[list[Message[T]]] = [[] for _ in range(partitions)]
        self._committed_offsets: Dict[str, list[int]] = {}

    def produce(self, key: str, payload: T) -> Message[T]:
        partition_id = self._partition_for_key(key)
        offset = len(self._partitions[partition_id])
        message = Message(partition=partition_id, offset=offset, key=key, payload=payload)
        self._partitions[partition_id].append(message)
        return message

    def register_group(self, group_name: str) -> None:
        if group_name not in self._committed_offsets:
            self._committed_offsets[group_name] = [0] * len(self._partitions)

    def poll(self, group_name: str, max_messages: int = 1) -> list[Message[T]]:
        if max_messages <= 0:
            raise ValueError("max_messages must be positive")

        committed = self._offsets_for_group(group_name)
        cursors = committed.copy()
        batch: list[Message[T]] = []

        while len(batch) < max_messages:
            emitted = False
            for partition_id, entries in enumerate(self._partitions):
                cursor = cursors[partition_id]
                if cursor >= len(entries):
                    continue

                batch.append(entries[cursor])
                cursors[partition_id] += 1
                emitted = True

                if len(batch) >= max_messages:
                    break

            if not emitted:
                break

        return batch

    def commit(self, group_name: str, messages: list[Message[T]]) -> None:
        committed = self._offsets_for_group(group_name)
        for message in messages:
            next_offset = message.offset + 1
            if next_offset > committed[message.partition]:
                committed[message.partition] = next_offset

    def lag(self, group_name: str) -> dict[int, int]:
        committed = self._offsets_for_group(group_name)
        return {
            partition_id: len(entries) - committed[partition_id]
            for partition_id, entries in enumerate(self._partitions)
        }

    def committed_offsets(self, group_name: str) -> dict[int, int]:
        committed = self._offsets_for_group(group_name)
        return {partition_id: offset for partition_id, offset in enumerate(committed)}

    def _offsets_for_group(self, group_name: str) -> list[int]:
        offsets = self._committed_offsets.get(group_name)
        if offsets is None:
            raise KeyError(f"unknown consumer group: {group_name}")
        return offsets

    def _partition_for_key(self, key: str) -> int:
        digest = hashlib.md5(key.encode("utf-8")).hexdigest()
        return int(digest, 16) % len(self._partitions)
