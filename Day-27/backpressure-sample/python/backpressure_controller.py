from __future__ import annotations

from collections import deque
from dataclasses import dataclass


@dataclass(frozen=True)
class WorkItem:
    id: str


class BoundedQueue:
    def __init__(self, capacity: int) -> None:
        if capacity <= 0:
            raise ValueError("capacity must be positive")
        self._capacity = capacity
        self._items: deque[WorkItem] = deque()

    def enqueue(self, item: WorkItem) -> bool:
        if len(self._items) >= self._capacity:
            return False
        self._items.append(item)
        return True

    def dequeue_many(self, count: int) -> list[WorkItem]:
        result: list[WorkItem] = []
        while count > 0 and self._items:
            result.append(self._items.popleft())
            count -= 1
        return result

    def depth(self) -> int:
        return len(self._items)

    def capacity(self) -> int:
        return self._capacity


class AdmissionController:
    def __init__(self, throttle_watermark: float, shed_watermark: float) -> None:
        if not (0 < throttle_watermark < shed_watermark < 1):
            raise ValueError("watermarks must satisfy 0 < throttle < shed < 1")
        self._throttle_watermark = throttle_watermark
        self._shed_watermark = shed_watermark

    def decide(self, queue: BoundedQueue, item_index: int) -> str:
        fill_ratio = queue.depth() / queue.capacity()

        if fill_ratio >= self._shed_watermark:
            return "SHED"
        if fill_ratio >= self._throttle_watermark:
            # Deterministic throttling: accept every 2nd item under pressure.
            return "ACCEPT" if item_index % 2 == 0 else "THROTTLE"
        return "ACCEPT"


class BackpressureEngine:
    def __init__(
        self,
        queue_capacity: int,
        consumer_rate_per_tick: int,
        throttle_watermark: float,
        shed_watermark: float,
    ) -> None:
        if consumer_rate_per_tick <= 0:
            raise ValueError("consumer_rate_per_tick must be positive")
        self.queue = BoundedQueue(queue_capacity)
        self.controller = AdmissionController(throttle_watermark, shed_watermark)
        self.consumer_rate = consumer_rate_per_tick

        self.accepted = 0
        self.throttled = 0
        self.shed = 0
        self.processed = 0
        self.depth_history: list[int] = []

    def tick(self, incoming_ids: list[str]) -> dict[str, int]:
        accepted_now = 0
        throttled_now = 0
        shed_now = 0

        for index, item_id in enumerate(incoming_ids):
            decision = self.controller.decide(self.queue, index)
            item = WorkItem(id=item_id)

            if decision == "ACCEPT":
                if self.queue.enqueue(item):
                    accepted_now += 1
                else:
                    shed_now += 1
            elif decision == "THROTTLE":
                throttled_now += 1
            else:
                shed_now += 1

        processed_now = len(self.queue.dequeue_many(self.consumer_rate))

        self.accepted += accepted_now
        self.throttled += throttled_now
        self.shed += shed_now
        self.processed += processed_now
        self.depth_history.append(self.queue.depth())

        return {
            "accepted_now": accepted_now,
            "throttled_now": throttled_now,
            "shed_now": shed_now,
            "processed_now": processed_now,
            "queue_depth": self.queue.depth(),
        }
