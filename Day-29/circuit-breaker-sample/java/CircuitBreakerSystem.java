import java.util.ArrayList;
import java.util.List;

public class CircuitBreakerSystem {
    public enum BreakerState {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    public static final class CallOutcome {
        private final BreakerState stateBefore;
        private final boolean allowed;
        private final String result;

        public CallOutcome(BreakerState stateBefore, boolean allowed, String result) {
            this.stateBefore = stateBefore;
            this.allowed = allowed;
            this.result = result;
        }

        public BreakerState getStateBefore() {
            return stateBefore;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getResult() {
            return result;
        }
    }

    public static final class CircuitBreaker {
        private final int failureThreshold;
        private final int cooldownTicks;
        private BreakerState state = BreakerState.CLOSED;
        private int consecutiveFailures = 0;
        private int cooldownRemaining = 0;

        public CircuitBreaker(int failureThreshold, int cooldownTicks) {
            if (failureThreshold <= 0) {
                throw new IllegalArgumentException("failureThreshold must be positive");
            }
            if (cooldownTicks <= 0) {
                throw new IllegalArgumentException("cooldownTicks must be positive");
            }
            this.failureThreshold = failureThreshold;
            this.cooldownTicks = cooldownTicks;
        }

        public boolean allow() {
            if (state == BreakerState.OPEN) {
                cooldownRemaining -= 1;
                if (cooldownRemaining <= 0) {
                    state = BreakerState.HALF_OPEN;
                    return true;
                }
                return false;
            }
            return true;
        }

        public void onSuccess() {
            consecutiveFailures = 0;
            cooldownRemaining = 0;
            state = BreakerState.CLOSED;
        }

        public void onFailure() {
            if (state == BreakerState.HALF_OPEN) {
                tripOpen();
                return;
            }

            consecutiveFailures += 1;
            if (consecutiveFailures >= failureThreshold) {
                tripOpen();
            }
        }

        private void tripOpen() {
            state = BreakerState.OPEN;
            cooldownRemaining = cooldownTicks;
            consecutiveFailures = 0;
        }

        public BreakerState getState() {
            return state;
        }
    }

    public static final class DependencySimulator {
        private final List<Boolean> results;
        private int index = 0;

        public DependencySimulator(List<Boolean> scriptedResults) {
            if (scriptedResults == null || scriptedResults.isEmpty()) {
                throw new IllegalArgumentException("scriptedResults must be non-empty");
            }
            this.results = new ArrayList<>(scriptedResults);
        }

        public boolean call() {
            if (index >= results.size()) {
                return results.get(results.size() - 1);
            }
            boolean outcome = results.get(index);
            index += 1;
            return outcome;
        }
    }

    public static final class GuardedClient {
        private final CircuitBreaker breaker;
        private final DependencySimulator dependency;
        private int success = 0;
        private int failure = 0;
        private int blocked = 0;

        public GuardedClient(CircuitBreaker breaker, DependencySimulator dependency) {
            this.breaker = breaker;
            this.dependency = dependency;
        }

        public CallOutcome request() {
            BreakerState before = breaker.getState();
            if (!breaker.allow()) {
                blocked += 1;
                return new CallOutcome(before, false, "BLOCKED");
            }

            boolean ok = dependency.call();
            if (ok) {
                breaker.onSuccess();
                success += 1;
                return new CallOutcome(before, true, "SUCCESS");
            }

            breaker.onFailure();
            failure += 1;
            return new CallOutcome(before, true, "FAILURE");
        }

        public CircuitBreaker getBreaker() {
            return breaker;
        }

        public int getSuccess() {
            return success;
        }

        public int getFailure() {
            return failure;
        }

        public int getBlocked() {
            return blocked;
        }
    }
}
