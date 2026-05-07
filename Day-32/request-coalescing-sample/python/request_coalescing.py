from __future__ import annotations

from dataclasses import dataclass
from enum import Enum


class ResolutionPath(Enum):
    CACHE_HIT = "CACHE_HIT"
    PRIMARY_FETCH = "PRIMARY_FETCH"
    COALESCED_WAIT = "COALESCED_WAIT"


@dataclass(frozen=True)
class RequestOutcome:
    request_id: int
    arrival_ms: int
    completion_ms: int
    latency_ms: int
    path: ResolutionPath


@dataclass(frozen=True)
class SimulationResult:
    outcomes: list[RequestOutcome]
    timeline: list[str]
    total_requests: int
    cache_hits: int
    coalesced_requests: int
    backend_calls: int
    backend_calls_saved: int
    average_latency_ms: float


@dataclass
class CacheEntry:
    value: str
    expires_at_ms: int


@dataclass
class InFlightFetch:
    value: str
    started_at_ms: int
    finishes_at_ms: int
    primary_request_id: int
    waiter_request_ids: list[int]


class RequestCoalescer:
    def simulate(
        self,
        request_arrivals_ms: list[int],
        backend_latencies_ms: list[int],
        ttl_ms: int,
    ) -> SimulationResult:
        if not request_arrivals_ms:
            raise ValueError("request_arrivals_ms must be non-empty")
        if any(request_arrivals_ms[index] < request_arrivals_ms[index - 1] for index in range(1, len(request_arrivals_ms))):
            raise ValueError("request_arrivals_ms must be non-decreasing")
        if any(latency <= 0 for latency in backend_latencies_ms):
            raise ValueError("backend_latencies_ms must contain positive values")
        if ttl_ms <= 0:
            raise ValueError("ttl_ms must be positive")

        timeline: list[str] = [f"simulation started ttl={ttl_ms}ms"]
        outcomes: dict[int, RequestOutcome] = {}

        cache: CacheEntry | None = None
        inflight: InFlightFetch | None = None
        backend_call_count = 0
        backend_latency_index = 0

        def complete_inflight_if_ready(current_time_ms: int) -> None:
            nonlocal inflight, cache
            if inflight is None or inflight.finishes_at_ms > current_time_ms:
                return

            finish = inflight.finishes_at_ms
            cache = CacheEntry(value=inflight.value, expires_at_ms=finish + ttl_ms)
            timeline.append(
                f"inflight completed at t={finish}ms cache_expires_at={cache.expires_at_ms}ms"
            )

            primary_id = inflight.primary_request_id
            primary_arrival = request_arrivals_ms[primary_id]
            outcomes[primary_id] = RequestOutcome(
                request_id=primary_id,
                arrival_ms=primary_arrival,
                completion_ms=finish,
                latency_ms=finish - primary_arrival,
                path=ResolutionPath.PRIMARY_FETCH,
            )

            for request_id in inflight.waiter_request_ids:
                arrival = request_arrivals_ms[request_id]
                outcomes[request_id] = RequestOutcome(
                    request_id=request_id,
                    arrival_ms=arrival,
                    completion_ms=finish,
                    latency_ms=finish - arrival,
                    path=ResolutionPath.COALESCED_WAIT,
                )

            inflight = None

        for request_id, arrival_ms in enumerate(request_arrivals_ms):
            complete_inflight_if_ready(arrival_ms)

            if cache is not None and arrival_ms < cache.expires_at_ms:
                timeline.append(f"r{request_id} t={arrival_ms}ms cache_hit")
                outcomes[request_id] = RequestOutcome(
                    request_id=request_id,
                    arrival_ms=arrival_ms,
                    completion_ms=arrival_ms,
                    latency_ms=0,
                    path=ResolutionPath.CACHE_HIT,
                )
                continue

            if inflight is not None and arrival_ms < inflight.finishes_at_ms:
                timeline.append(
                    f"r{request_id} t={arrival_ms}ms joined_inflight "
                    f"(finishes_at={inflight.finishes_at_ms}ms)"
                )
                inflight.waiter_request_ids.append(request_id)
                continue

            latency = backend_latencies_ms[min(backend_latency_index, len(backend_latencies_ms) - 1)]
            backend_latency_index += 1
            backend_call_count += 1
            finish_ms = arrival_ms + latency
            inflight = InFlightFetch(
                value=f"value_v{backend_call_count}",
                started_at_ms=arrival_ms,
                finishes_at_ms=finish_ms,
                primary_request_id=request_id,
                waiter_request_ids=[],
            )
            timeline.append(
                f"r{request_id} t={arrival_ms}ms started_backend_fetch "
                f"(latency={latency}ms finish={finish_ms}ms)"
            )

        complete_inflight_if_ready(10**9)

        ordered_outcomes = [outcomes[index] for index in sorted(outcomes.keys())]
        if len(ordered_outcomes) != len(request_arrivals_ms):
            raise RuntimeError("internal error: missing request outcomes")

        cache_hits = sum(1 for outcome in ordered_outcomes if outcome.path == ResolutionPath.CACHE_HIT)
        coalesced = sum(1 for outcome in ordered_outcomes if outcome.path == ResolutionPath.COALESCED_WAIT)
        avg_latency = sum(outcome.latency_ms for outcome in ordered_outcomes) / len(ordered_outcomes)
        backend_calls_saved = coalesced

        timeline.append(
            f"finished total={len(ordered_outcomes)} cache_hits={cache_hits} "
            f"coalesced={coalesced} backend_calls={backend_call_count} "
            f"saved_calls={backend_calls_saved}"
        )
        return SimulationResult(
            outcomes=ordered_outcomes,
            timeline=timeline,
            total_requests=len(ordered_outcomes),
            cache_hits=cache_hits,
            coalesced_requests=coalesced,
            backend_calls=backend_call_count,
            backend_calls_saved=backend_calls_saved,
            average_latency_ms=avg_latency,
        )
