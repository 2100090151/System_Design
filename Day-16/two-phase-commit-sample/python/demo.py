from two_phase_commit import TwoPhaseCommitCoordinator


def print_tx_state(coordinator: TwoPhaseCommitCoordinator, tx_id: str) -> None:
    for line in coordinator.states_for_tx(tx_id):
        print("  " + line)


def main() -> None:
    coordinator = TwoPhaseCommitCoordinator(["inventory", "payment", "shipping"])

    tx_id = "tx-100"
    operations = {
        "inventory": "reserve sku=book-1 qty=1",
        "payment": "charge user=42 amount=499",
        "shipping": "create shipment user=42",
    }
    decision = coordinator.execute(tx_id, operations)
    print(f"{tx_id} decision: {decision.value}")
    print_tx_state(coordinator, tx_id)

    tx_id = "tx-101"
    coordinator.participant("payment").set_reject_for_tx(tx_id, True)
    decision = coordinator.execute(tx_id, operations)
    print(f"\n{tx_id} decision (payment votes NO): {decision.value}")
    print_tx_state(coordinator, tx_id)
    coordinator.participant("payment").set_reject_for_tx(tx_id, False)

    tx_id = "tx-102"
    coordinator.participant("shipping").set_available(True)
    decision = coordinator.execute(tx_id, operations, drop_before_decision=["shipping"])
    print(f"\n{tx_id} decision (shipping misses decision broadcast): {decision.value}")
    print_tx_state(coordinator, tx_id)

    coordinator.participant("shipping").set_available(True)
    coordinator.reconcile_participant("shipping", tx_id)
    print(f"\n{tx_id} after shipping recovery + reconcile:")
    print_tx_state(coordinator, tx_id)


if __name__ == "__main__":
    main()
