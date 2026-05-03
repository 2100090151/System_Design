from bulkhead_isolation import BulkheadEngine, BulkheadPool, Task


def main() -> None:
    pools = {
        "critical": BulkheadPool("critical", queue_capacity=5, workers_per_tick=2),
        "best_effort": BulkheadPool("best_effort", queue_capacity=3, workers_per_tick=1),
    }
    engine = BulkheadEngine(pools)

    batch_1 = [
        Task("c-1", "critical"),
        Task("c-2", "critical"),
        Task("b-1", "best_effort"),
        Task("b-2", "best_effort"),
        Task("b-3", "best_effort"),
        Task("b-4", "best_effort"),
    ]

    batch_2 = [
        Task("c-3", "critical"),
        Task("c-4", "critical"),
        Task("c-5", "critical"),
        Task("c-6", "critical"),
        Task("b-5", "best_effort"),
    ]

    print("submit batch 1")
    for line in engine.submit_batch(batch_1):
        print(line)

    print("\ntick 1")
    for line in engine.tick():
        print(line)

    print("\nsubmit batch 2")
    for line in engine.submit_batch(batch_2):
        print(line)

    print("\ntick 2")
    for line in engine.tick():
        print(line)

    print("\nfinal metrics")
    metrics = engine.metrics()
    for lane in sorted(metrics.keys()):
        print(lane, metrics[lane])


if __name__ == "__main__":
    main()
