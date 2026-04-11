from idempotency_store import IdempotencyStore


class PaymentService:
    def __init__(self) -> None:
        self._processed_count = 0

    def charge(self, customer_id: str, amount_cents: int) -> str:
        self._processed_count += 1
        payment_id = f"pay-{self._processed_count:04d}"
        return (
            f"payment_id={payment_id}, customer_id={customer_id}, "
            f"amount_cents={amount_cents}"
        )

    @property
    def processed_count(self) -> int:
        return self._processed_count


def main() -> None:
    store = IdempotencyStore[str]()
    payments = PaymentService()

    attempts = [
        ("payment-req-101", "cust-7", 2500),
        ("payment-req-101", "cust-7", 2500),
        ("payment-req-102", "cust-7", 4100),
        ("payment-req-102", "cust-7", 4100),
    ]

    for key, customer_id, amount_cents in attempts:
        result = store.execute(
            key,
            lambda customer_id=customer_id, amount_cents=amount_cents: payments.charge(
                customer_id, amount_cents
            ),
        )
        status = "replayed" if result.replayed else "processed"
        print(f"{key}: {status} -> {result.value}")

    print()
    print("Business operations executed:", payments.processed_count)
    print("Stored idempotency keys:", store.processed_keys())


if __name__ == "__main__":
    main()
