from bloom_filter import BloomFilter


def print_check(filter_: BloomFilter, item: str) -> None:
    verdict = "might be present" if filter_.might_contain(item) else "definitely not present"
    print(f"{item:>12} -> {verdict}")


def main() -> None:
    bloom = BloomFilter(size_bits=128, hash_count=3)

    inserted_items = [
        "user:1001",
        "user:1002",
        "user:1003",
        "order:9001",
        "order:9002",
    ]

    print("adding items")
    for item in inserted_items:
        bloom.add(item)
        print(f"  added {item}")

    print("\nmembership checks")
    for item in ["user:1001", "user:5000", "order:9002", "order:9999"]:
        print_check(bloom, item)

    print("\nfilter stats")
    print(f"  fill ratio: {bloom.fill_ratio():.2%}")
    print(f"  estimated false positive rate: {bloom.estimated_false_positive_rate():.4%}")


if __name__ == "__main__":
    main()
