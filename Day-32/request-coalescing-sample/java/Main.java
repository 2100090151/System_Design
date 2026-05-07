import java.util.List;

public class Main {
    private static void printResult(String title, List<Integer> arrivals, List<Integer> latencies, int ttlMs) {
        RequestCoalescingSystem.RequestCoalescer coalescer = new RequestCoalescingSystem.RequestCoalescer();
        RequestCoalescingSystem.SimulationResult result = coalescer.simulate(arrivals, latencies, ttlMs);

        System.out.println(title);
        System.out.println(
            "  total="
                + result.getTotalRequests()
                + " cache_hits="
                + result.getCacheHits()
                + " coalesced="
                + result.getCoalescedRequests()
                + " backend_calls="
                + result.getBackendCalls()
                + " saved_calls="
                + result.getBackendCallsSaved()
                + " avg_latency="
                + String.format("%.1f", result.getAverageLatencyMs())
                + "ms"
        );

        for (RequestCoalescingSystem.RequestOutcome outcome : result.getOutcomes()) {
            System.out.println(
                "  r"
                    + outcome.getRequestId()
                    + ": arrival="
                    + outcome.getArrivalMs()
                    + "ms completion="
                    + outcome.getCompletionMs()
                    + "ms latency="
                    + outcome.getLatencyMs()
                    + "ms path="
                    + outcome.getPath()
            );
        }

        for (String line : result.getTimeline()) {
            System.out.println("  " + line);
        }
    }

    public static void main(String[] args) {
        printResult(
            "burst after cache expiry (stampede protected by one-flight)",
            List.of(0, 2, 4, 6, 40),
            List.of(25, 30),
            30
        );

        System.out.println();
        printResult(
            "spaced traffic (little coalescing opportunity)",
            List.of(0, 35, 70),
            List.of(20, 20, 20),
            10
        );
    }
}
