from __future__ import annotations

from collections import defaultdict
from dataclasses import dataclass, field
from threading import Lock
from typing import Callable


Handler = Callable[[str, dict], None]


@dataclass
class EventBus:
    _subscribers: dict[str, list[Handler]] = field(default_factory=lambda: defaultdict(list), init=False)
    _lock: Lock = field(default_factory=Lock, init=False)

    def subscribe(self, topic: str, handler: Handler) -> None:
        if not topic:
            raise ValueError("Topic must not be empty")

        with self._lock:
            self._subscribers[topic].append(handler)

    def publish(self, topic: str, payload: dict) -> int:
        with self._lock:
            handlers = list(self._subscribers.get(topic, []))

        for handler in handlers:
            handler(topic, payload)

        return len(handlers)

    def topic_snapshot(self) -> dict[str, int]:
        with self._lock:
            return {topic: len(handlers) for topic, handlers in self._subscribers.items()}
