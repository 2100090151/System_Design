from time import sleep

from token_bucket import TokenBucketRateLimiter


def main() -> None:
    limiter = TokenBucketRateLimiter(capacity=5, refill_rate_per_second=2.0)

    print("Initial tokens:", round(limiter.available_tokens(), 2))
    print("Burst of 7 requests:")
    for i in range(1, 8):
        allowed = limiter.allow_request()
        print(f"  request-{i}: {'allowed' if allowed else 'rejected'} | tokens={limiter.available_tokens():.2f}")

    print("\nWaiting 2 seconds for refill...")
    sleep(2)

    print("Next 4 requests:")
    for i in range(8, 12):
        allowed = limiter.allow_request()
        print(f"  request-{i}: {'allowed' if allowed else 'rejected'} | tokens={limiter.available_tokens():.2f}")


if __name__ == "__main__":
    main()
