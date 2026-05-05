import java.util.ArrayList;
import java.util.List;

public class TimeoutControlSystem {
    public enum ResultStatus {
        SUCCESS,
        TIMEOUT
    }

    public static final class ServiceStep {
        private final String name;
        private final int durationMs;

        public ServiceStep(String name, int durationMs) {
            this.name = name;
            this.durationMs = durationMs;
        }

        public String getName() {
            return name;
        }

        public int getDurationMs() {
            return durationMs;
        }
    }

    public static final class RequestContext {
        private final int deadlineMs;
        private int elapsedMs = 0;
        private boolean cancelled = false;

        public RequestContext(int deadlineMs) {
            this.deadlineMs = deadlineMs;
        }

        public int remainingMs() {
            return Math.max(0, deadlineMs - elapsedMs);
        }

        public int getElapsedMs() {
            return elapsedMs;
        }

        public void addElapsedMs(int delta) {
            elapsedMs += delta;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public void cancel() {
            cancelled = true;
        }
    }

    public static final class ExecutionResult {
        private final ResultStatus status;
        private final int elapsedMs;
        private final List<String> timeline;

        public ExecutionResult(ResultStatus status, int elapsedMs, List<String> timeline) {
            this.status = status;
            this.elapsedMs = elapsedMs;
            this.timeline = timeline;
        }

        public ResultStatus getStatus() {
            return status;
        }

        public int getElapsedMs() {
            return elapsedMs;
        }

        public List<String> getTimeline() {
            return timeline;
        }
    }

    public static final class DeadlineExecutor {
        public ExecutionResult execute(List<ServiceStep> steps, int deadlineMs) {
            if (deadlineMs <= 0) {
                throw new IllegalArgumentException("deadlineMs must be positive");
            }
            if (steps == null || steps.isEmpty()) {
                throw new IllegalArgumentException("steps must be non-empty");
            }

            RequestContext context = new RequestContext(deadlineMs);
            List<String> timeline = new ArrayList<>();
            timeline.add("request started with deadline=" + deadlineMs + "ms");

            for (ServiceStep step : steps) {
                if (context.isCancelled()) {
                    timeline.add(step.getName() + ": skipped (cancelled)");
                    continue;
                }

                int remaining = context.remainingMs();
                timeline.add(step.getName() + ": remaining_before=" + remaining + "ms");

                if (remaining <= 0) {
                    context.cancel();
                    timeline.add(step.getName() + ": timeout before execution");
                    continue;
                }

                if (step.getDurationMs() > remaining) {
                    context.addElapsedMs(remaining);
                    context.cancel();
                    timeline.add(
                        step.getName()
                            + ": timed out after consuming remaining "
                            + remaining
                            + "ms (needed "
                            + step.getDurationMs()
                            + "ms)"
                    );
                    continue;
                }

                context.addElapsedMs(step.getDurationMs());
                timeline.add(
                    step.getName()
                        + ": success duration="
                        + step.getDurationMs()
                        + "ms elapsed="
                        + context.getElapsedMs()
                        + "ms"
                );
            }

            ResultStatus status = context.isCancelled() ? ResultStatus.TIMEOUT : ResultStatus.SUCCESS;
            timeline.add("request finished status=" + status + " elapsed=" + context.getElapsedMs() + "ms");
            return new ExecutionResult(status, context.getElapsedMs(), timeline);
        }
    }
}
