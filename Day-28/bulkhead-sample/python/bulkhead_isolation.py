from __future__ import annotations

from collections import deque
from dataclasses import dataclass


@dataclass(frozen=True)
class Task:
    task_id: str
    lane: str


class BulkheadPool:
    def __init__(self, lane: str, queue_capacity: int, workers_per_tick: int) -> None:
        self.lane = lane
        self._capacity = queue_capacity
        self._workers_per_tick = workers_per_tick
        self._queue: deque[Task] = deque()

        self.accepted = 0
        self.rejected = 0
        self.processed = 0

    def submit(self, task: Task) -> bool:
        if len(self._queue) >= self._capacity:
            self.rejected += 1
            return False
        self._queue.append(task)
        self.accepted += 1
        return True

    def process_tick(self) -> list[str]:
        history: list[str] = []
        for _ in range(self._workers_per_tick):
            if not self._queue:
                break
            task = self._queue.popleft()
            self.processed += 1
            history.append(f"processed {task.task_id} lane={self.lane}")
        return history

    def queue_depth(self) -> int:
        return len(self._queue)


class BulkheadRouter:
    def __init__(self, pools: dict[str, BulkheadPool]) -> None:
        self._pools = pools

    def route(self, task: Task) -> bool:
        pool = self._pools.get(task.lane)
        if pool is None:
            return False
        return pool.submit(task)


class BulkheadEngine:
    def __init__(self, pools: dict[str, BulkheadPool]) -> None:
        self._pools = pools
        self._router = BulkheadRouter(pools)

    def submit_batch(self, tasks: list[Task]) -> list[str]:
        history: list[str] = []
        for task in tasks:
            ok = self._router.route(task)
            history.append(("accepted" if ok else "rejected") + f" {task.task_id} lane={task.lane}")
        return history

    def tick(self) -> list[str]:
        history: list[str] = []
        for lane in sorted(self._pools.keys()):
            history.extend(self._pools[lane].process_tick())
        return history

    def metrics(self) -> dict[str, dict[str, int]]:
        result: dict[str, dict[str, int]] = {}
        for lane, pool in self._pools.items():
            result[lane] = {
                "accepted": pool.accepted,
                "rejected": pool.rejected,
                "processed": pool.processed,
                "queue_depth": pool.queue_depth(),
            }
        return result
