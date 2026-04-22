from __future__ import annotations

from dataclasses import dataclass
from enum import Enum
from typing import Protocol


class Severity(Enum):
    INFO = "INFO"
    WARNING = "WARNING"
    CRITICAL = "CRITICAL"


@dataclass(frozen=True)
class Alert:
    alert_id: str
    severity: Severity
    message: str


class AlertWriter(Protocol):
    def save(self, alert: Alert) -> None: ...


class AlertReader(Protocol):
    def list_all(self) -> list[Alert]: ...


class AlertChannel(Protocol):
    name: str

    def supports(self, alert: Alert) -> bool: ...

    def send(self, alert: Alert) -> None: ...

    def sent_ids(self) -> list[str]: ...


class InMemoryAlertRepository(AlertWriter, AlertReader):
    def __init__(self) -> None:
        self._alerts: list[Alert] = []

    def save(self, alert: Alert) -> None:
        self._alerts.append(alert)

    def list_all(self) -> list[Alert]:
        return list(self._alerts)


class EmailChannel(AlertChannel):
    name = "email"

    def __init__(self) -> None:
        self._sent: list[str] = []

    def supports(self, alert: Alert) -> bool:
        return True

    def send(self, alert: Alert) -> None:
        self._sent.append(alert.alert_id)

    def sent_ids(self) -> list[str]:
        return list(self._sent)


class SmsChannel(AlertChannel):
    name = "sms"

    def __init__(self) -> None:
        self._sent: list[str] = []

    def supports(self, alert: Alert) -> bool:
        return alert.severity in {Severity.WARNING, Severity.CRITICAL}

    def send(self, alert: Alert) -> None:
        self._sent.append(alert.alert_id)

    def sent_ids(self) -> list[str]:
        return list(self._sent)


class PagerDutyChannel(AlertChannel):
    name = "pager-duty"

    def __init__(self) -> None:
        self._sent: list[str] = []

    def supports(self, alert: Alert) -> bool:
        return alert.severity == Severity.CRITICAL

    def send(self, alert: Alert) -> None:
        self._sent.append(alert.alert_id)

    def sent_ids(self) -> list[str]:
        return list(self._sent)


class AlertDispatcher:
    def __init__(self, writer: AlertWriter, channels: list[AlertChannel]) -> None:
        if not channels:
            raise ValueError("channels must be non-empty")
        self._writer = writer
        self._channels = list(channels)

    def dispatch(self, alert: Alert) -> list[str]:
        self._writer.save(alert)
        routed: list[str] = []

        for channel in self._channels:
            if channel.supports(alert):
                channel.send(alert)
                routed.append(channel.name)

        return routed


class AlertAuditService:
    def __init__(self, reader: AlertReader) -> None:
        self._reader = reader

    def critical_count(self) -> int:
        return sum(1 for alert in self._reader.list_all() if alert.severity == Severity.CRITICAL)
