from __future__ import annotations

from collections import deque
from dataclasses import dataclass


@dataclass(frozen=True)
class WorkMessage:
    message_id: str
    payload: str
    attempt: int = 1


class Worker:
    """Simulates processing outcomes.

    - payload='ok' succeeds immediately
    - payload='flaky' fails first 2 attempts then succeeds
    - payload='poison' always fails
    """

    def process(self, message: WorkMessage) -> bool:
        if message.payload == "ok":
            return True
        if message.payload == "flaky":
            return message.attempt >= 3
        if message.payload == "poison":
            return False
        return False


class DeadLetterQueue:
    def __init__(self) -> None:
        self.messages: list[WorkMessage] = []

    def add(self, message: WorkMessage) -> None:
        self.messages.append(message)


class RetryEngine:
    def __init__(self, worker: Worker, max_attempts: int = 3) -> None:
        if max_attempts < 1:
            raise ValueError("max_attempts must be >= 1")
        self._worker = worker
        self._max_attempts = max_attempts
        self._queue: deque[WorkMessage] = deque()
        self.dlq = DeadLetterQueue()

        self.success_count = 0
        self.retry_count = 0
        self.dlq_count = 0

    def enqueue(self, message: WorkMessage) -> None:
        self._queue.append(message)

    def run(self) -> list[str]:
        history: list[str] = []

        while self._queue:
            message = self._queue.popleft()
            ok = self._worker.process(message)
            if ok:
                self.success_count += 1
                history.append(f"success {message.message_id} attempt={message.attempt}")
                continue

            if message.attempt >= self._max_attempts:
                self.dlq.add(message)
                self.dlq_count += 1
                history.append(f"dlq {message.message_id} attempt={message.attempt}")
                continue

            next_message = WorkMessage(
                message_id=message.message_id,
                payload=message.payload,
                attempt=message.attempt + 1,
            )
            self._queue.append(next_message)
            self.retry_count += 1
            history.append(f"retry {message.message_id} next_attempt={next_message.attempt}")

        return history
