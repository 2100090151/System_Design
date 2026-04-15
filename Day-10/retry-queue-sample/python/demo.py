from retry_queue import RetryQueue


def print_state(queue: RetryQueue[str], label: str) -> None:
    print(label)
    print("  queue:", queue.queue_ids())
    print("  inflight:", queue.inflight_ids())
    print("  dlq:", queue.dead_letter_ids())


def main() -> None:
    queue = RetryQueue[str](max_retries=1, capacity=2)

    print("enqueue invoice-101 ->", queue.enqueue("invoice-101"))
    print("enqueue invoice-102 ->", queue.enqueue("invoice-102"))
    print("enqueue invoice-103 (capacity exceeded) ->", queue.enqueue("invoice-103"))
    print_state(queue, "state after enqueue")

    first = queue.poll()
    if first:
        print("poll ->", first.job_id, first.payload)
        print("ack ->", queue.ack(first.job_id))
    print_state(queue, "state after first job success")

    second = queue.poll()
    if second:
        print("poll ->", second.job_id, second.payload)
        print("fail ->", queue.fail(second.job_id))
    print_state(queue, "state after first failure")

    retry = queue.poll()
    if retry:
        print("poll retry ->", retry.job_id, retry.payload)
        print("fail again ->", queue.fail(retry.job_id))
    print_state(queue, "final state")


if __name__ == "__main__":
    main()