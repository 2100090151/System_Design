from __future__ import annotations

from dataclasses import dataclass
from uuid import uuid4


@dataclass(frozen=True)
class TraceContext:
    trace_id: str
    span_id: str
    parent_span_id: str | None = None

    @staticmethod
    def start_root() -> "TraceContext":
        return TraceContext(trace_id=uuid4().hex, span_id=uuid4().hex[:16])

    @staticmethod
    def from_headers(headers: dict[str, str]) -> "TraceContext":
        trace_id = headers["x-trace-id"]
        span_id = headers["x-span-id"]
        parent_span_id = headers.get("x-parent-span-id") or None
        return TraceContext(trace_id=trace_id, span_id=span_id, parent_span_id=parent_span_id)

    def create_child(self) -> "TraceContext":
        return TraceContext(
            trace_id=self.trace_id,
            span_id=uuid4().hex[:16],
            parent_span_id=self.span_id,
        )

    def to_headers(self) -> dict[str, str]:
        headers = {
            "x-trace-id": self.trace_id,
            "x-span-id": self.span_id,
        }
        if self.parent_span_id is not None:
            headers["x-parent-span-id"] = self.parent_span_id
        return headers

    def format_log(self, service: str, message: str) -> str:
        parent = self.parent_span_id or "-"
        return (
            f"[service={service}] [trace_id={self.trace_id}] "
            f"[span_id={self.span_id}] [parent_span_id={parent}] {message}"
        )
