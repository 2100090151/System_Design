from quorum_kv import QuorumKVStore


def print_states(store: QuorumKVStore, key: str) -> None:
    for line in store.states_for_key(key):
        print("  " + line)


def main() -> None:
    key = "user:42"
    store = QuorumKVStore(
        replica_names=["replica-a", "replica-b", "replica-c"],
        read_quorum=2,
        write_quorum=2,
    )

    print("initial write with all replicas available")
    print(f"  write success: {store.write(key, 'active')}")
    print_states(store, key)

    print("\nbring replica-c down and write a newer value")
    store.set_replica_availability("replica-c", False)
    print(f"  write success: {store.write(key, 'suspended')}")
    print_states(store, key)

    print("\nbring replica-c back and read the key")
    store.set_replica_availability("replica-c", True)
    value, version, repaired = store.read(key)
    print(f"  read value={value}, version={version}, repaired_replicas={repaired}")
    print_states(store, key)

    print("\nforce quorum failure and attempt write")
    store.set_replica_availability("replica-b", False)
    store.set_replica_availability("replica-c", False)
    print(f"  write success: {store.write(key, 'closed')}")
    print_states(store, key)


if __name__ == "__main__":
    main()
