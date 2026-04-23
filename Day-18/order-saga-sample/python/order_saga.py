from __future__ import annotations

from dataclasses import dataclass
from enum import Enum


class SagaStatus(Enum):
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"


@dataclass(frozen=True)
class SagaResult:
    status: SagaStatus
    history: list[str]


class SagaStep:
    def __init__(self, name: str) -> None:
        self.name = name

    def execute(self) -> bool:
        raise NotImplementedError

    def compensate(self) -> bool:
        raise NotImplementedError


class OrderSagaOrchestrator:
    def __init__(self, steps: list[SagaStep]) -> None:
        if not steps:
            raise ValueError("steps must be non-empty")
        self._steps = steps[:]

    def run(self) -> SagaResult:
        completed: list[SagaStep] = []
        history: list[str] = []

        for step in self._steps:
            history.append(f"execute {step.name}")
            ok = step.execute()
            history.append(f"result {step.name}: {'OK' if ok else 'FAIL'}")
            if not ok:
                history.append(f"saga failed at {step.name}; starting compensation")
                for completed_step in reversed(completed):
                    history.append(f"compensate {completed_step.name}")
                    compensated = completed_step.compensate()
                    history.append(
                        f"compensation {completed_step.name}: {'OK' if compensated else 'FAIL'}"
                    )
                return SagaResult(status=SagaStatus.FAILED, history=history)
            completed.append(step)

        history.append("saga completed")
        return SagaResult(status=SagaStatus.COMPLETED, history=history)


class InventoryStep(SagaStep):
    def __init__(self) -> None:
        super().__init__("inventory")
        self._reserved = False

    def execute(self) -> bool:
        self._reserved = True
        return True

    def compensate(self) -> bool:
        if self._reserved:
            self._reserved = False
        return True


class PaymentStep(SagaStep):
    def __init__(self, should_fail: bool = False) -> None:
        super().__init__("payment")
        self._should_fail = should_fail
        self._charged = False

    def execute(self) -> bool:
        if self._should_fail:
            return False
        self._charged = True
        return True

    def compensate(self) -> bool:
        if self._charged:
            self._charged = False
        return True


class ShippingStep(SagaStep):
    def __init__(self, should_fail: bool = False) -> None:
        super().__init__("shipping")
        self._should_fail = should_fail
        self._created = False

    def execute(self) -> bool:
        if self._should_fail:
            return False
        self._created = True
        return True

    def compensate(self) -> bool:
        if self._created:
            self._created = False
        return True
