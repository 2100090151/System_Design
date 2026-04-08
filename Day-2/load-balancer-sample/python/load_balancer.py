from __future__ import annotations

from dataclasses import dataclass, field
from threading import Lock


@dataclass
class BackendServer:
    name: str
    healthy: bool = True


@dataclass
class RoundRobinLoadBalancer:
    _servers: list[BackendServer] = field(default_factory=list, init=False)
    _index: int = field(default=0, init=False)
    _lock: Lock = field(default_factory=Lock, init=False)

    def add_server(self, name: str) -> None:
        with self._lock:
            self._servers.append(BackendServer(name=name))

    def set_health(self, name: str, healthy: bool) -> None:
        with self._lock:
            for server in self._servers:
                if server.name == name:
                    server.healthy = healthy
                    return
        raise ValueError(f"Unknown server: {name}")

    def next_server(self) -> str:
        with self._lock:
            if not self._servers:
                raise RuntimeError("No backend servers registered")

            total = len(self._servers)
            for _ in range(total):
                server = self._servers[self._index]
                self._index = (self._index + 1) % total
                if server.healthy:
                    return server.name

        raise RuntimeError("No healthy backend servers available")

    def snapshot(self) -> list[tuple[str, bool]]:
        with self._lock:
            return [(server.name, server.healthy) for server in self._servers]
