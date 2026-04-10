public class Main {
    public static void main(String[] args) throws InterruptedException {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(5, 2.0);

        System.out.printf("Initial tokens: %.2f%n", limiter.availableTokens());
        System.out.println("Burst of 7 requests:");
        for (int i = 1; i <= 7; i++) {
            boolean allowed = limiter.allowRequest(1.0);
            System.out.printf("  request-%d: %s | tokens=%.2f%n",
                    i,
                    allowed ? "allowed" : "rejected",
                    limiter.availableTokens());
        }

        System.out.println();
        System.out.println("Waiting 2 seconds for refill...");
        Thread.sleep(2000);

        System.out.println("Next 4 requests:");
        for (int i = 8; i <= 11; i++) {
            boolean allowed = limiter.allowRequest(1.0);
            System.out.printf("  request-%d: %s | tokens=%.2f%n",
                    i,
                    allowed ? "allowed" : "rejected",
                    limiter.availableTokens());
        }
    }
}
