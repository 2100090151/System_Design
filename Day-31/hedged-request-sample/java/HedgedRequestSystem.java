import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HedgedRequestSystem {
    public enum ResultStatus {
        SUCCESS,
        FAILED
    }

    public static final class ReplicaEndpoint {
        private final String name;
        private final int latencyMs;
        private final boolean willSucceed;

        public ReplicaEndpoint(String name, int latencyMs, boolean willSucceed) {
            this.name = name;
            this.latencyMs = latencyMs;
            this.willSucceed = willSucceed;
        }

        public String getName() {
            return name;
        }

        public int getLatencyMs() {
            return latencyMs;
        }

        public boolean willSucceed() {
            return willSucceed;
        }
    }

    public static final class AttemptRecord {
        private final String endpointName;
        private final int startMs;
        private final int finishMs;
        private final boolean willSucceed;
        private final int sequence;

        public AttemptRecord(String endpointName, int startMs, int finishMs, boolean willSucceed, int sequence) {
            this.endpointName = endpointName;
            this.startMs = startMs;
            this.finishMs = finishMs;
            this.willSucceed = willSucceed;
            this.sequence = sequence;
        }

        public String getEndpointName() {
            return endpointName;
        }

        public int getStartMs() {
            return startMs;
        }

        public int getFinishMs() {
            return finishMs;
        }

        public boolean willSucceed() {
            return willSucceed;
        }

        public int getSequence() {
            return sequence;
        }
    }

    public static final class ExecutionResult {
        private final ResultStatus status;
        private final String winner;
        private final int completionMs;
        private final int duplicatesIssued;
        private final int tailSavedMs;
        private final List<String> timeline;

        public ExecutionResult(
            ResultStatus status,
            String winner,
            int completionMs,
            int duplicatesIssued,
            int tailSavedMs,
            List<String> timeline
        ) {
            this.status = status;
            this.winner = winner;
            this.completionMs = completionMs;
            this.duplicatesIssued = duplicatesIssued;
            this.tailSavedMs = tailSavedMs;
            this.timeline = timeline;
        }

        public ResultStatus getStatus() {
            return status;
        }

        public String getWinner() {
            return winner;
        }

        public int getCompletionMs() {
            return completionMs;
        }

        public int getDuplicatesIssued() {
            return duplicatesIssued;
        }

        public int getTailSavedMs() {
            return tailSavedMs;
        }

        public List<String> getTimeline() {
            return timeline;
        }
    }

    public static final class HedgedRequestExecutor {
        public ExecutionResult execute(List<ReplicaEndpoint> replicas, int hedgeDelayMs) {
            if (hedgeDelayMs <= 0) {
                throw new IllegalArgumentException("hedgeDelayMs must be positive");
            }
            if (replicas == null || replicas.size() < 2) {
                throw new IllegalArgumentException("replicas must contain at least two endpoints");
            }

            List<AttemptRecord> attempts = new ArrayList<>();
            for (int i = 0; i < replicas.size(); i++) {
                ReplicaEndpoint replica = replicas.get(i);
                if (replica.getLatencyMs() <= 0) {
                    throw new IllegalArgumentException("replica latencyMs must be positive");
                }
                int startMs = i == 0 ? 0 : hedgeDelayMs * i;
                attempts.add(
                    new AttemptRecord(
                        replica.getName(),
                        startMs,
                        startMs + replica.getLatencyMs(),
                        replica.willSucceed(),
                        i
                    )
                );
            }

            AttemptRecord winner = null;
            for (AttemptRecord attempt : attempts) {
                if (!attempt.willSucceed()) {
                    continue;
                }
                if (winner == null) {
                    winner = attempt;
                    continue;
                }
                if (attempt.getFinishMs() < winner.getFinishMs()) {
                    winner = attempt;
                } else if (
                    attempt.getFinishMs() == winner.getFinishMs()
                        && attempt.getSequence() < winner.getSequence()
                ) {
                    winner = attempt;
                }
            }

            Integer winnerTime = winner == null ? null : winner.getFinishMs();
            List<AttemptRecord> launchedAttempts = new ArrayList<>();
            if (winnerTime == null) {
                launchedAttempts.addAll(attempts);
            } else {
                for (AttemptRecord attempt : attempts) {
                    if (attempt == winner || attempt.getStartMs() < winnerTime) {
                        launchedAttempts.add(attempt);
                    }
                }
            }

            int duplicatesIssued = Math.max(0, launchedAttempts.size() - 1);
            AttemptRecord primary = attempts.get(0);
            int completionMs = winnerTime != null
                ? winnerTime
                : launchedAttempts.stream().mapToInt(AttemptRecord::getFinishMs).max().orElse(0);
            int tailSavedMs = winnerTime == null ? 0 : Math.max(0, primary.getFinishMs() - winnerTime);

            List<String> timeline = new ArrayList<>();
            timeline.add("request started hedge_delay=" + hedgeDelayMs + "ms");
            for (AttemptRecord attempt : attempts) {
                boolean launched = launchedAttempts.contains(attempt);
                if (launched) {
                    timeline.add(
                        attempt.getEndpointName()
                            + ": launched at t="
                            + attempt.getStartMs()
                            + "ms expected_finish="
                            + attempt.getFinishMs()
                            + "ms"
                    );
                } else {
                    timeline.add(
                        attempt.getEndpointName()
                            + ": not launched (winner decided at "
                            + winnerTime
                            + "ms)"
                    );
                }
            }

            launchedAttempts.sort(
                Comparator
                    .comparingInt(AttemptRecord::getFinishMs)
                    .thenComparingInt(AttemptRecord::getSequence)
            );
            for (AttemptRecord attempt : launchedAttempts) {
                if (winner != null && attempt == winner) {
                    timeline.add(attempt.getEndpointName() + ": winner success at t=" + attempt.getFinishMs() + "ms");
                } else if (winnerTime != null && attempt.getFinishMs() > winnerTime) {
                    timeline.add(
                        attempt.getEndpointName()
                            + ": cancelled at winner_time="
                            + winnerTime
                            + "ms before finish="
                            + attempt.getFinishMs()
                            + "ms"
                    );
                } else if (attempt.willSucceed()) {
                    timeline.add(attempt.getEndpointName() + ": success at t=" + attempt.getFinishMs() + "ms (lost race)");
                } else {
                    timeline.add(attempt.getEndpointName() + ": failed at t=" + attempt.getFinishMs() + "ms");
                }
            }

            ResultStatus status = winner == null ? ResultStatus.FAILED : ResultStatus.SUCCESS;
            timeline.add(
                "request finished status="
                    + status
                    + " completion="
                    + completionMs
                    + "ms duplicates_issued="
                    + duplicatesIssued
                    + " tail_saved="
                    + tailSavedMs
                    + "ms"
            );

            return new ExecutionResult(
                status,
                winner == null ? null : winner.getEndpointName(),
                completionMs,
                duplicatesIssued,
                tailSavedMs,
                timeline
            );
        }
    }
}
