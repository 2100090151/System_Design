public class TokenBucketRateLimiter {
    private final int capacity;
    private final double refillRatePerSecond;
    private double tokens;
    private long lastRefillNanos;

    public TokenBucketRateLimiter(int capacity, double refillRatePerSecond) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        if (refillRatePerSecond <= 0) {
            throw new IllegalArgumentException("refillRatePerSecond must be positive");
        }

        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
        this.tokens = capacity;
        this.lastRefillNanos = System.nanoTime();
    }

    public synchronized boolean allowRequest(double tokensRequired) {
        if (tokensRequired <= 0) {
            throw new IllegalArgumentException("tokensRequired must be positive");
        }

        refill();
        if (tokens >= tokensRequired) {
            tokens -= tokensRequired;
            return true;
        }
        return false;
    }

    public synchronized double availableTokens() {
        refill();
        return tokens;
    }

    private void refill() {
        long now = System.nanoTime();
        long elapsedNanos = now - lastRefillNanos;
        if (elapsedNanos <= 0) {
            return;
        }

        double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
        double refilled = elapsedSeconds * refillRatePerSecond;
        tokens = Math.min(capacity, tokens + refilled);
        lastRefillNanos = now;
    }
}
