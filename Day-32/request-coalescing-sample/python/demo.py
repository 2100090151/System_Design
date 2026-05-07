from request_coalescing import RequestCoalescer


def print_result(title: str, arrivals: list[int], latencies: list[int], ttl_ms: int) -> None:
    coalescer = RequestCoalescer()
    result = coalescer.simulate(arrivals, latencies, ttl_ms)

    print(title)
    print(
        f"  total={result.total_requests} cache_hits={result.cache_hits} "
        f"coalesced={result.coalesced_requests} backend_calls={result.backend_calls} "
        f"saved_calls={result.backend_calls_saved} avg_latency={result.average_latency_ms:.1f}ms"
    )
    for outcome in result.outcomes:
        print(
            f"  r{outcome.request_id}: arrival={outcome.arrival_ms}ms "
            f"completion={outcome.completion_ms}ms latency={outcome.latency_ms}ms "
            f"path={outcome.path.value}"
        )
    for line in result.timeline:
        print(" ", line)


def main() -> None:
    print_result(
        "burst after cache expiry (stampede protected by one-flight)",
        arrivals=[0, 2, 4, 6, 40],
        latencies=[25, 30],
        ttl_ms=30,
    )

    print()
    print_result(
        "spaced traffic (little coalescing opportunity)",
        arrivals=[0, 35, 70],
        latencies=[20, 20, 20],
        ttl_ms=10,
    )


if __name__ == "__main__":
    main()
