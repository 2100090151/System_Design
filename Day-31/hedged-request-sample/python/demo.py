from hedged_request import HedgedRequestExecutor, ReplicaEndpoint


def print_result(title: str, replicas: list[ReplicaEndpoint], hedge_delay_ms: int) -> None:
    executor = HedgedRequestExecutor()
    result = executor.execute(replicas=replicas, hedge_delay_ms=hedge_delay_ms)
    print(title)
    print(
        f"  status={result.status.value} winner={result.winner} "
        f"completion={result.completion_ms}ms duplicates={result.duplicates_issued} "
        f"tail_saved={result.tail_saved_ms}ms"
    )
    for line in result.timeline:
        print(" ", line)


def main() -> None:
    slow_primary = [
        ReplicaEndpoint("primary-us-east", 220, True),
        ReplicaEndpoint("replica-us-west", 90, True),
        ReplicaEndpoint("replica-eu", 130, True),
    ]
    print_result("hedge beats slow primary", slow_primary, hedge_delay_ms=80)

    print()
    fast_primary = [
        ReplicaEndpoint("primary-us-east", 70, True),
        ReplicaEndpoint("replica-us-west", 90, True),
        ReplicaEndpoint("replica-eu", 110, True),
    ]
    print_result("fast primary returns before hedge launch", fast_primary, hedge_delay_ms=80)

    print()
    all_fail = [
        ReplicaEndpoint("primary-us-east", 100, False),
        ReplicaEndpoint("replica-us-west", 120, False),
        ReplicaEndpoint("replica-eu", 60, False),
    ]
    print_result("all attempts fail", all_fail, hedge_delay_ms=50)


if __name__ == "__main__":
    main()
