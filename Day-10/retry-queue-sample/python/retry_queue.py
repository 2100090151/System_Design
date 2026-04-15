from __future__ import annotations

from collections import deque
from dataclasses import dataclass, field
from typing import Deque, Dict, Generic, TypeVar

T = TypeVar("T")


@dataclass
class Job(Generic[T]):
    job_id: str
    payload: T
    attempts: int = 0


@dataclass
class RetryQueue(Generic[T]):
    max_retries: int
    capacity: int
    _counter: int = field(default=0, init=False)
    _queue: Deque[Job[T]] = field(default_factory=deque, init=False)
    _inflight: Dict[str, Job[T]] = field(default_factory=dict, init=False)
    _dead_letter: list[Job[T]] = field(default_factory=list, init=False)

    def __post_init__(self) -> None:
        if self.max_retries < 0:
            raise ValueError("max_retries must be >= 0")
        if self.capacity <= 0:
            raise ValueError("capacity must be positive")

    def enqueue(self, payload: T) -> str | None:
        if len(self._queue) >= self.capacity:
            return None

        self._counter += 1
        job = Job(job_id=f"job-{self._counter}", payload=payload)
        self._queue.append(job)
        return job.job_id

    def poll(self) -> Job[T] | None:
        if not self._queue:
            return None

        job = self._queue.popleft()
        self._inflight[job.job_id] = job
        return job

    def ack(self, job_id: str) -> bool:
        return self._inflight.pop(job_id, None) is not None

    def fail(self, job_id: str) -> str:
        job = self._inflight.pop(job_id, None)
        if job is None:
            return "missing"

        job.attempts += 1
        if job.attempts <= self.max_retries:
            self._queue.append(job)
            return "requeued"

        self._dead_letter.append(job)
        return "dead-lettered"

    def queue_ids(self) -> list[str]:
        return [job.job_id for job in self._queue]

    def inflight_ids(self) -> list[str]:
        return list(self._inflight.keys())

    def dead_letter_ids(self) -> list[str]:
        return [job.job_id for job in self._dead_letter]