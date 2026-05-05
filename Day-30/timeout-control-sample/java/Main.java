import java.util.List;

public class Main {
    private static void printResult(String title, List<TimeoutControlSystem.ServiceStep> steps, int deadlineMs) {
        TimeoutControlSystem.DeadlineExecutor executor = new TimeoutControlSystem.DeadlineExecutor();
        TimeoutControlSystem.ExecutionResult result = executor.execute(steps, deadlineMs);

        System.out.println(title);
        System.out.println("  status=" + result.getStatus() + " elapsed=" + result.getElapsedMs() + "ms");
        for (String line : result.getTimeline()) {
            System.out.println("  " + line);
        }
    }

    public static void main(String[] args) {
        List<TimeoutControlSystem.ServiceStep> fastChain = List.of(
            new TimeoutControlSystem.ServiceStep("auth-service", 20),
            new TimeoutControlSystem.ServiceStep("profile-service", 25),
            new TimeoutControlSystem.ServiceStep("inventory-service", 30)
        );
        printResult("successful request chain", fastChain, 100);

        List<TimeoutControlSystem.ServiceStep> slowChain = List.of(
            new TimeoutControlSystem.ServiceStep("auth-service", 20),
            new TimeoutControlSystem.ServiceStep("recommendation-service", 70),
            new TimeoutControlSystem.ServiceStep("pricing-service", 20)
        );
        System.out.println();
        printResult("timeout with cancellation propagation", slowChain, 80);
    }
}
