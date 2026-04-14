from lru_cache import LRUCache


def print_state(cache: LRUCache[str, str], label: str) -> None:
    print(label, cache.snapshot())


def main() -> None:
    cache = LRUCache[str, str](capacity=3)

    print("put A=alpha | evicted:", cache.put("A", "alpha"))
    print_state(cache, "  state:")

    print("put B=bravo | evicted:", cache.put("B", "bravo"))
    print_state(cache, "  state:")

    print("put C=charlie | evicted:", cache.put("C", "charlie"))
    print_state(cache, "  state:")

    print("get A ->", cache.get("A"))
    print_state(cache, "  state after touching A:")

    print("put D=delta | evicted:", cache.put("D", "delta"))
    print_state(cache, "  state:")

    print("get B ->", cache.get("B"))
    print("get C ->", cache.get("C"))
    print_state(cache, "  final state:")


if __name__ == "__main__":
    main()
