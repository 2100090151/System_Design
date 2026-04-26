from event_sourcing_account import CommandService, EventStore, SnapshotStore


def print_aggregate(prefix: str, agg) -> None:
    print(
        prefix,
        f"account={agg.account_id} balance={agg.balance_cents} version={agg.version} seq={agg.last_sequence}",
    )


def main() -> None:
    events = EventStore()
    snapshots = SnapshotStore()
    commands = CommandService(events, snapshots)

    account_id = "acct-100"

    commands.open_account(account_id)
    commands.deposit(account_id, 10_000)
    commands.deposit(account_id, 2_500)
    commands.withdraw(account_id, 1_200)

    full_before_snapshot = commands.rebuild_full(account_id)
    print_aggregate("before snapshot:", full_before_snapshot)

    snap = commands.snapshot(account_id)
    print(
        "snapshot created:",
        f"balance={snap.balance_cents} version={snap.version} seq={snap.last_sequence}",
    )

    commands.deposit(account_id, 500)
    commands.withdraw(account_id, 300)

    full = commands.rebuild_full(account_id)
    with_snapshot = commands.rebuild_with_snapshot(account_id)

    print_aggregate("full rebuild:", full)
    print_aggregate("snapshot rebuild:", with_snapshot)

    print("event count:", len(events.all_events()))
    print("state match:", full.balance_cents == with_snapshot.balance_cents)


if __name__ == "__main__":
    main()
