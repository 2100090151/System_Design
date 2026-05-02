from backpressure_controller import BackpressureEngine


def main() -> None:
    engine = BackpressureEngine(
        queue_capacity=10,
        consumer_rate_per_tick=3,
        throttle_watermark=0.6,
        shed_watermark=0.9,
    )

    incoming_batches = [
        ["m1", "m2", "m3", "m4"],
        ["m5", "m6", "m7", "m8", "m9", "m10", "m11"],
        ["m12", "m13", "m14", "m15", "m16", "m17"],
        ["m18", "m19", "m20"],
        ["m21", "m22", "m23", "m24", "m25"],
    ]

    print("tick results")
    for tick_index, batch in enumerate(incoming_batches, start=1):
        result = engine.tick(batch)
        print(f"  tick={tick_index} in={len(batch)} -> {result}")

    print("\nsummary")
    print(f"  accepted={engine.accepted}")
    print(f"  throttled={engine.throttled}")
    print(f"  shed={engine.shed}")
    print(f"  processed={engine.processed}")
    print(f"  final_queue_depth={engine.queue.depth()}")
    print(f"  depth_history={engine.depth_history}")


if __name__ == "__main__":
    main()
