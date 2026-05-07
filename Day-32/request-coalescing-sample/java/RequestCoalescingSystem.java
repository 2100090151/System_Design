import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestCoalescingSystem {
    public enum ResolutionPath {
        CACHE_HIT,
        PRIMARY_FETCH,
        COALESCED_WAIT
    }

    public static final class RequestOutcome {
        private final int requestId;
        private final int arrivalMs;
        private final int completionMs;
        private final int latencyMs;
        private final ResolutionPath path;

        public RequestOutcome(int requestId, int arrivalMs, int completionMs, int latencyMs, ResolutionPath path) {
            this.requestId = requestId;
            this.arrivalMs = arrivalMs;
            this.completionMs = completionMs;
            this.latencyMs = latencyMs;
            this.path = path;
        }

        public int getRequestId() {
            return requestId;
        }

        public int getArrivalMs() {
            return arrivalMs;
        }

        public int getCompletionMs() {
            return completionMs;
        }

        public int getLatencyMs() {
            return latencyMs;
        }

        public ResolutionPath getPath() {
            return path;
        }
    }

    public static final class SimulationResult {
        private final List<RequestOutcome> outcomes;
        private final List<String> timeline;
        private final int totalRequests;
        private final int cacheHits;
        private final int coalescedRequests;
        private final int backendCalls;
        private final int backendCallsSaved;
        private final double averageLatencyMs;

        public SimulationResult(
            List<RequestOutcome> outcomes,
            List<String> timeline,
            int totalRequests,
            int cacheHits,
            int coalescedRequests,
            int backendCalls,
            int backendCallsSaved,
            double averageLatencyMs
        ) {
            this.outcomes = outcomes;
            this.timeline = timeline;
            this.totalRequests = totalRequests;
            this.cacheHits = cacheHits;
            this.coalescedRequests = coalescedRequests;
            this.backendCalls = backendCalls;
            this.backendCallsSaved = backendCallsSaved;
            this.averageLatencyMs = averageLatencyMs;
        }

        public List<RequestOutcome> getOutcomes() {
            return outcomes;
        }

        public List<String> getTimeline() {
            return timeline;
        }

        public int getTotalRequests() {
            return totalRequests;
        }

        public int getCacheHits() {
            return cacheHits;
        }

        public int getCoalescedRequests() {
            return coalescedRequests;
        }

        public int getBackendCalls() {
            return backendCalls;
        }

        public int getBackendCallsSaved() {
            return backendCallsSaved;
        }

        public double getAverageLatencyMs() {
            return averageLatencyMs;
        }
    }

    private static final class CacheEntry {
        private final String value;
        private final int expiresAtMs;

        private CacheEntry(String value, int expiresAtMs) {
            this.value = value;
            this.expiresAtMs = expiresAtMs;
        }
    }

    private static final class InFlightFetch {
        private final String value;
        private final int startedAtMs;
        private final int finishesAtMs;
        private final int primaryRequestId;
        private final List<Integer> waiterRequestIds;

        private InFlightFetch(
            String value,
            int startedAtMs,
            int finishesAtMs,
            int primaryRequestId,
            List<Integer> waiterRequestIds
        ) {
            this.value = value;
            this.startedAtMs = startedAtMs;
            this.finishesAtMs = finishesAtMs;
            this.primaryRequestId = primaryRequestId;
            this.waiterRequestIds = waiterRequestIds;
        }
    }

    public static final class RequestCoalescer {
        public SimulationResult simulate(List<Integer> requestArrivalsMs, List<Integer> backendLatenciesMs, int ttlMs) {
            if (requestArrivalsMs == null || requestArrivalsMs.isEmpty()) {
                throw new IllegalArgumentException("requestArrivalsMs must be non-empty");
            }
            for (int i = 1; i < requestArrivalsMs.size(); i++) {
                if (requestArrivalsMs.get(i) < requestArrivalsMs.get(i - 1)) {
                    throw new IllegalArgumentException("requestArrivalsMs must be non-decreasing");
                }
            }
            if (backendLatenciesMs == null || backendLatenciesMs.isEmpty()) {
                throw new IllegalArgumentException("backendLatenciesMs must be non-empty");
            }
            for (int latency : backendLatenciesMs) {
                if (latency <= 0) {
                    throw new IllegalArgumentException("backendLatenciesMs must contain positive values");
                }
            }
            if (ttlMs <= 0) {
                throw new IllegalArgumentException("ttlMs must be positive");
            }

            List<String> timeline = new ArrayList<>();
            timeline.add("simulation started ttl=" + ttlMs + "ms");
            Map<Integer, RequestOutcome> outcomes = new HashMap<>();

            Holder holder = new Holder();
            holder.cache = null;
            holder.inflight = null;
            int backendCallCount = 0;
            int backendLatencyIndex = 0;

            for (int requestId = 0; requestId < requestArrivalsMs.size(); requestId++) {
                int arrivalMs = requestArrivalsMs.get(requestId);
                completeInFlightIfReady(arrivalMs, ttlMs, requestArrivalsMs, outcomes, timeline, holder);

                if (holder.cache != null && arrivalMs < holder.cache.expiresAtMs) {
                    timeline.add("r" + requestId + " t=" + arrivalMs + "ms cache_hit");
                    outcomes.put(
                        requestId,
                        new RequestOutcome(requestId, arrivalMs, arrivalMs, 0, ResolutionPath.CACHE_HIT)
                    );
                    continue;
                }

                if (holder.inflight != null && arrivalMs < holder.inflight.finishesAtMs) {
                    timeline.add(
                        "r"
                            + requestId
                            + " t="
                            + arrivalMs
                            + "ms joined_inflight (finishes_at="
                            + holder.inflight.finishesAtMs
                            + "ms)"
                    );
                    holder.inflight.waiterRequestIds.add(requestId);
                    continue;
                }

                int latency = backendLatenciesMs.get(Math.min(backendLatencyIndex, backendLatenciesMs.size() - 1));
                backendLatencyIndex += 1;
                backendCallCount += 1;
                int finishMs = arrivalMs + latency;
                holder.inflight = new InFlightFetch(
                    "value_v" + backendCallCount,
                    arrivalMs,
                    finishMs,
                    requestId,
                    new ArrayList<>()
                );
                timeline.add(
                    "r"
                        + requestId
                        + " t="
                        + arrivalMs
                        + "ms started_backend_fetch (latency="
                        + latency
                        + "ms finish="
                        + finishMs
                        + "ms)"
                );
            }

            completeInFlightIfReady(1_000_000_000, ttlMs, requestArrivalsMs, outcomes, timeline, holder);

            List<RequestOutcome> ordered = new ArrayList<>();
            for (int i = 0; i < requestArrivalsMs.size(); i++) {
                RequestOutcome outcome = outcomes.get(i);
                if (outcome == null) {
                    throw new IllegalStateException("internal error: missing request outcomes");
                }
                ordered.add(outcome);
            }

            int cacheHits = 0;
            int coalesced = 0;
            int totalLatency = 0;
            for (RequestOutcome outcome : ordered) {
                totalLatency += outcome.getLatencyMs();
                if (outcome.getPath() == ResolutionPath.CACHE_HIT) {
                    cacheHits += 1;
                } else if (outcome.getPath() == ResolutionPath.COALESCED_WAIT) {
                    coalesced += 1;
                }
            }

            int savedCalls = coalesced;
            double avgLatency = (double) totalLatency / ordered.size();
            timeline.add(
                "finished total="
                    + ordered.size()
                    + " cache_hits="
                    + cacheHits
                    + " coalesced="
                    + coalesced
                    + " backend_calls="
                    + backendCallCount
                    + " saved_calls="
                    + savedCalls
            );

            return new SimulationResult(
                ordered,
                timeline,
                ordered.size(),
                cacheHits,
                coalesced,
                backendCallCount,
                savedCalls,
                avgLatency
            );
        }

        private static void completeInFlightIfReady(
            int currentTimeMs,
            int ttlMs,
            List<Integer> requestArrivalsMs,
            Map<Integer, RequestOutcome> outcomes,
            List<String> timeline,
            Holder holder
        ) {
            if (holder.inflight == null || holder.inflight.finishesAtMs > currentTimeMs) {
                return;
            }

            int finish = holder.inflight.finishesAtMs;
            holder.cache = new CacheEntry(holder.inflight.value, finish + ttlMs);
            timeline.add(
                "inflight completed at t="
                    + finish
                    + "ms cache_expires_at="
                    + holder.cache.expiresAtMs
                    + "ms"
            );

            int primaryId = holder.inflight.primaryRequestId;
            int primaryArrival = requestArrivalsMs.get(primaryId);
            outcomes.put(
                primaryId,
                new RequestOutcome(
                    primaryId,
                    primaryArrival,
                    finish,
                    finish - primaryArrival,
                    ResolutionPath.PRIMARY_FETCH
                )
            );

            for (int requestId : holder.inflight.waiterRequestIds) {
                int arrival = requestArrivalsMs.get(requestId);
                outcomes.put(
                    requestId,
                    new RequestOutcome(
                        requestId,
                        arrival,
                        finish,
                        finish - arrival,
                        ResolutionPath.COALESCED_WAIT
                    )
                );
            }

            holder.inflight = null;
        }
    }

    private static final class Holder {
        private CacheEntry cache;
        private InFlightFetch inflight;
    }
}
