public class Main {
    public static void main(String[] args) {
        IdempotencyStore<String> store = new IdempotencyStore<>();
        PaymentService payments = new PaymentService();

        processAttempt(store, payments, "payment-req-101", "cust-7", 2500);
        processAttempt(store, payments, "payment-req-101", "cust-7", 2500);
        processAttempt(store, payments, "payment-req-102", "cust-7", 4100);
        processAttempt(store, payments, "payment-req-102", "cust-7", 4100);

        System.out.println();
        System.out.println("Business operations executed: " + payments.getProcessedCount());
        System.out.println("Stored idempotency keys: " + store.processedKeys());
    }

    private static void processAttempt(
            IdempotencyStore<String> store,
            PaymentService payments,
            String key,
            String customerId,
            int amountCents
    ) {
        IdempotencyStore.ProcessResult<String> result =
                store.execute(key, () -> payments.charge(customerId, amountCents));

        String status = result.replayed() ? "replayed" : "processed";
        System.out.printf("%s: %s -> %s%n", key, status, result.value());
    }

    private static final class PaymentService {
        private int processedCount = 0;

        public String charge(String customerId, int amountCents) {
            processedCount++;
            String paymentId = String.format("pay-%04d", processedCount);
            return String.format(
                    "payment_id=%s, customer_id=%s, amount_cents=%d",
                    paymentId,
                    customerId,
                    amountCents
            );
        }

        public int getProcessedCount() {
            return processedCount;
        }
    }
}
