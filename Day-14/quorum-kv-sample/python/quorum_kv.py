from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class VersionedValue:
    version: int
    value: str


class Replica:
    def __init__(self, name: str) -> None:
        self.name = name
        self.available = True
        self._store: dict[str, VersionedValue] = {}

    def set_available(self, available: bool) -> None:
        self.available = available

    def read(self, key: str) -> VersionedValue | None:
        return self._store.get(key)

    def apply_write(self, key: str, version: int, value: str) -> None:
        self._store[key] = VersionedValue(version=version, value=value)


class QuorumKVStore:
    def __init__(self, replica_names: list[str], read_quorum: int, write_quorum: int) -> None:
        if not replica_names:
            raise ValueError("replica_names must be non-empty")
        if read_quorum <= 0 or write_quorum <= 0:
            raise ValueError("read_quorum and write_quorum must be positive")
        if read_quorum > len(replica_names) or write_quorum > len(replica_names):
            raise ValueError("quorum values must be <= number of replicas")

        self._replicas = [Replica(name) for name in replica_names]
        self._read_quorum = read_quorum
        self._write_quorum = write_quorum
        self._next_version_by_key: dict[str, int] = {}

    def set_replica_availability(self, replica_name: str, available: bool) -> None:
        replica = self._find_replica(replica_name)
        replica.set_available(available)

    def write(self, key: str, value: str) -> bool:
        self._validate_key_value(key, value)
        available_replicas = self._available_replicas()
        if len(available_replicas) < self._write_quorum:
            return False

        version = self._next_version_by_key.get(key, 0) + 1
        self._next_version_by_key[key] = version
        for replica in available_replicas:
            replica.apply_write(key, version, value)
        return True

    def read(self, key: str) -> tuple[str | None, int | None, int]:
        if not key:
            raise ValueError("key must be non-empty")

        available_replicas = self._available_replicas()
        if len(available_replicas) < self._read_quorum:
            raise RuntimeError("read quorum not met")

        latest: VersionedValue | None = None
        for replica in available_replicas:
            current = replica.read(key)
            if current is None:
                continue
            if latest is None or current.version > latest.version:
                latest = current

        if latest is None:
            return None, None, 0

        repaired = 0
        for replica in available_replicas:
            current = replica.read(key)
            if current is None or current.version < latest.version:
                replica.apply_write(key, latest.version, latest.value)
                repaired += 1

        return latest.value, latest.version, repaired

    def states_for_key(self, key: str) -> list[str]:
        lines: list[str] = []
        for replica in self._replicas:
            current = replica.read(key)
            if current is None:
                value_repr = "None"
                version_repr = "-"
            else:
                value_repr = current.value
                version_repr = str(current.version)

            lines.append(
                f"{replica.name}: available={replica.available}, version={version_repr}, value={value_repr}"
            )
        return lines

    def _available_replicas(self) -> list[Replica]:
        return [replica for replica in self._replicas if replica.available]

    def _find_replica(self, replica_name: str) -> Replica:
        for replica in self._replicas:
            if replica.name == replica_name:
                return replica
        raise ValueError(f"unknown replica: {replica_name}")

    @staticmethod
    def _validate_key_value(key: str, value: str) -> None:
        if not key:
            raise ValueError("key must be non-empty")
        if not value:
            raise ValueError("value must be non-empty")
