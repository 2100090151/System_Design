import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderSaga {
    public enum SagaStatus {
        COMPLETED,
        FAILED
    }

    public static final class SagaResult {
        private final SagaStatus status;
        private final List<String> history;

        private SagaResult(SagaStatus status, List<String> history) {
            this.status = status;
            this.history = history;
        }

        public SagaStatus getStatus() {
            return status;
        }

        public List<String> getHistory() {
            return history;
        }
    }

    public interface SagaStep {
        String name();
        boolean execute();
        boolean compensate();
    }

    public static final class OrderSagaOrchestrator {
        private final List<SagaStep> steps;

        public OrderSagaOrchestrator(List<SagaStep> steps) {
            if (steps == null || steps.isEmpty()) {
                throw new IllegalArgumentException("steps must be non-empty");
            }
            this.steps = new ArrayList<>(steps);
        }

        public SagaResult run() {
            List<SagaStep> completed = new ArrayList<>();
            List<String> history = new ArrayList<>();

            for (SagaStep step : steps) {
                history.add("execute " + step.name());
                boolean ok = step.execute();
                history.add("result " + step.name() + ": " + (ok ? "OK" : "FAIL"));
                if (!ok) {
                    history.add("saga failed at " + step.name() + "; starting compensation");
                    Collections.reverse(completed);
                    for (SagaStep completedStep : completed) {
                        history.add("compensate " + completedStep.name());
                        boolean compensated = completedStep.compensate();
                        history.add(
                            "compensation "
                                + completedStep.name()
                                + ": "
                                + (compensated ? "OK" : "FAIL")
                        );
                    }
                    return new SagaResult(SagaStatus.FAILED, history);
                }
                completed.add(step);
            }

            history.add("saga completed");
            return new SagaResult(SagaStatus.COMPLETED, history);
        }
    }

    public static final class InventoryStep implements SagaStep {
        private boolean reserved = false;

        @Override
        public String name() {
            return "inventory";
        }

        @Override
        public boolean execute() {
            reserved = true;
            return true;
        }

        @Override
        public boolean compensate() {
            if (reserved) {
                reserved = false;
            }
            return true;
        }
    }

    public static final class PaymentStep implements SagaStep {
        private final boolean shouldFail;
        private boolean charged = false;

        public PaymentStep(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }

        @Override
        public String name() {
            return "payment";
        }

        @Override
        public boolean execute() {
            if (shouldFail) {
                return false;
            }
            charged = true;
            return true;
        }

        @Override
        public boolean compensate() {
            if (charged) {
                charged = false;
            }
            return true;
        }
    }

    public static final class ShippingStep implements SagaStep {
        private final boolean shouldFail;
        private boolean created = false;

        public ShippingStep(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }

        @Override
        public String name() {
            return "shipping";
        }

        @Override
        public boolean execute() {
            if (shouldFail) {
                return false;
            }
            created = true;
            return true;
        }

        @Override
        public boolean compensate() {
            if (created) {
                created = false;
            }
            return true;
        }
    }
}
