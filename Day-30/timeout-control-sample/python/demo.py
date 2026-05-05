from timeout_control import DeadlineExecutor, ServiceStep


def print_result(title: str, steps: list[ServiceStep], deadline_ms: int) -> None:
    executor = DeadlineExecutor()
    result = executor.execute(steps=steps, deadline_ms=deadline_ms)
    print(title)
    print(f"  status={result.status.value} elapsed={result.elapsed_ms}ms")
    for line in result.timeline:
        print(" ", line)


def main() -> None:
    fast_chain = [
        ServiceStep("auth-service", 20),
        ServiceStep("profile-service", 25),
        ServiceStep("inventory-service", 30),
    ]
    print_result("successful request chain", fast_chain, deadline_ms=100)

    slow_chain = [
        ServiceStep("auth-service", 20),
        ServiceStep("recommendation-service", 70),
        ServiceStep("pricing-service", 20),
    ]
    print()
    print_result("timeout with cancellation propagation", slow_chain, deadline_ms=80)


if __name__ == "__main__":
    main()
