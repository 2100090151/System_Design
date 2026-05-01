from retry_dlq import RetryEngine, WorkMessage, Worker


def main() -> None:
    engine = RetryEngine(worker=Worker(), max_attempts=3)

    engine.enqueue(WorkMessage("msg-ok-1", "ok"))
    engine.enqueue(WorkMessage("msg-flaky-1", "flaky"))
    engine.enqueue(WorkMessage("msg-poison-1", "poison"))

    for line in engine.run():
        print(line)

    print("success_count:", engine.success_count)
    print("retry_count:", engine.retry_count)
    print("dlq_count:", engine.dlq_count)
    print("dlq_ids:", [m.message_id for m in engine.dlq.messages])


if __name__ == "__main__":
    main()
