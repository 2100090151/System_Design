import java.util.List;

public class Main {
    private static void printResult(String title, List<HedgedRequestSystem.ReplicaEndpoint> replicas, int hedgeDelayMs) {
        HedgedRequestSystem.HedgedRequestExecutor executor = new HedgedRequestSystem.HedgedRequestExecutor();
        HedgedRequestSystem.ExecutionResult result = executor.execute(replicas, hedgeDelayMs);

        System.out.println(title);
        System.out.println(
            "  status="
                + result.getStatus()
                + " winner="
                + result.getWinner()
                + " completion="
                + result.getCompletionMs()
                + "ms duplicates="
                + result.getDuplicatesIssued()
                + " tail_saved="
                + result.getTailSavedMs()
                + "ms"
        );
        for (String line : result.getTimeline()) {
            System.out.println("  " + line);
        }
    }

    public static void main(String[] args) {
        List<HedgedRequestSystem.ReplicaEndpoint> slowPrimary = List.of(
            new HedgedRequestSystem.ReplicaEndpoint("primary-us-east", 220, true),
            new HedgedRequestSystem.ReplicaEndpoint("replica-us-west", 90, true),
            new HedgedRequestSystem.ReplicaEndpoint("replica-eu", 130, true)
        );
        printResult("hedge beats slow primary", slowPrimary, 80);

        System.out.println();
        List<HedgedRequestSystem.ReplicaEndpoint> fastPrimary = List.of(
            new HedgedRequestSystem.ReplicaEndpoint("primary-us-east", 70, true),
            new HedgedRequestSystem.ReplicaEndpoint("replica-us-west", 90, true),
            new HedgedRequestSystem.ReplicaEndpoint("replica-eu", 110, true)
        );
        printResult("fast primary returns before hedge launch", fastPrimary, 80);

        System.out.println();
        List<HedgedRequestSystem.ReplicaEndpoint> allFail = List.of(
            new HedgedRequestSystem.ReplicaEndpoint("primary-us-east", 100, false),
            new HedgedRequestSystem.ReplicaEndpoint("replica-us-west", 120, false),
            new HedgedRequestSystem.ReplicaEndpoint("replica-eu", 60, false)
        );
        printResult("all attempts fail", allFail, 50);
    }
}
