from consistent_hashing import ConsistentHashRing


def main() -> None:
    keys = [f"user:{i}" for i in range(1, 21)]

    ring = ConsistentHashRing(virtual_nodes=20)
    ring.add_node("shard-a")
    ring.add_node("shard-b")
    ring.add_node("shard-c")

    print("Initial routing:")
    for key in keys[:8]:
        print(f"  {key} -> {ring.get_node(key)}")
    print("Distribution:", ring.distribution(keys))

    ring.add_node("shard-d")
    print("\nAfter adding shard-d:")
    for key in keys[:8]:
        print(f"  {key} -> {ring.get_node(key)}")
    print("Distribution:", ring.distribution(keys))


if __name__ == "__main__":
    main()
