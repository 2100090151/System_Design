from order_saga import (
    InventoryStep,
    OrderSagaOrchestrator,
    PaymentStep,
    ShippingStep,
)


def print_result(title: str, orchestrator: OrderSagaOrchestrator) -> None:
    print(title)
    result = orchestrator.run()
    print("  status:", result.status.value)
    for line in result.history:
        print(" ", line)


def main() -> None:
    success_saga = OrderSagaOrchestrator(
        [InventoryStep(), PaymentStep(), ShippingStep()]
    )
    print_result("successful saga", success_saga)

    failed_saga = OrderSagaOrchestrator(
        [InventoryStep(), PaymentStep(), ShippingStep(should_fail=True)]
    )
    print("\n")
    print_result("failed saga with compensation", failed_saga)


if __name__ == "__main__":
    main()
