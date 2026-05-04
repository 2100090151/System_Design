from circuit_breaker import CircuitBreaker, DependencySimulator, GuardedClient


def main() -> None:
    # False means dependency failure, True means success.
    scripted = [False, False, False, False, True, True, False, True]
    breaker = CircuitBreaker(failure_threshold=3, cooldown_ticks=2)
    dependency = DependencySimulator(scripted)
    client = GuardedClient(breaker, dependency)

    print("tick results")
    for tick in range(1, 11):
        outcome = client.request()
        print(
            f"  tick={tick} before={outcome.state_before.value} "
            f"allowed={outcome.allowed} result={outcome.result} "
            f"after={client.breaker.state.value}"
        )

    print("\nsummary")
    print(f"  success={client.success}")
    print(f"  failure={client.failure}")
    print(f"  blocked={client.blocked}")
    print(f"  final_state={client.breaker.state.value}")


if __name__ == "__main__":
    main()
