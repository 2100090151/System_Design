from __future__ import annotations

from dataclasses import dataclass
from enum import Enum


class ResultStatus(Enum):
    SUCCESS = "SUCCESS"
    TIMEOUT = "TIMEOUT"


@dataclass(frozen=True)
class ServiceStep:
    name: str
    duration_ms: int


@dataclass
class RequestContext:
    deadline_ms: int
    elapsed_ms: int = 0
    cancelled: bool = False

    def remaining_ms(self) -> int:
        return max(0, self.deadline_ms - self.elapsed_ms)


@dataclass(frozen=True)
class ExecutionResult:
    status: ResultStatus
    elapsed_ms: int
    timeline: list[str]


class DeadlineExecutor:
    def execute(self, steps: list[ServiceStep], deadline_ms: int) -> ExecutionResult:
        if deadline_ms <= 0:
            raise ValueError("deadline_ms must be positive")
        if not steps:
            raise ValueError("steps must be non-empty")

        context = RequestContext(deadline_ms=deadline_ms)
        timeline: list[str] = [f"request started with deadline={deadline_ms}ms"]

        for step in steps:
            if context.cancelled:
                timeline.append(f"{step.name}: skipped (cancelled)")
                continue

            remaining = context.remaining_ms()
            timeline.append(f"{step.name}: remaining_before={remaining}ms")

            if remaining <= 0:
                context.cancelled = True
                timeline.append(f"{step.name}: timeout before execution")
                continue

            if step.duration_ms > remaining:
                context.elapsed_ms += remaining
                context.cancelled = True
                timeline.append(
                    f"{step.name}: timed out after consuming remaining {remaining}ms "
                    f"(needed {step.duration_ms}ms)"
                )
                continue

            context.elapsed_ms += step.duration_ms
            timeline.append(
                f"{step.name}: success duration={step.duration_ms}ms elapsed={context.elapsed_ms}ms"
            )

        status = ResultStatus.TIMEOUT if context.cancelled else ResultStatus.SUCCESS
        timeline.append(f"request finished status={status.value} elapsed={context.elapsed_ms}ms")
        return ExecutionResult(status=status, elapsed_ms=context.elapsed_ms, timeline=timeline)
